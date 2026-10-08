package com.chavez.store.api_rest.service.impl;

import com.chavez.store.api_rest.dto.request.AjusteStockRequestDTO;
import com.chavez.store.api_rest.dto.request.LoteInicialDTO;
import com.chavez.store.api_rest.dto.request.MermaRequestDTO;
import com.chavez.store.api_rest.dto.request.StockInicialRequestDTO;
import com.chavez.store.api_rest.dto.response.DescuadreDTO;
import com.chavez.store.api_rest.dto.response.KardexResponseDTO;
import com.chavez.store.api_rest.dto.response.LoteAfectadoDTO;
import com.chavez.store.api_rest.dto.response.LoteStockResponseDTO;
import com.chavez.store.api_rest.dto.response.MermaResponseDTO;
import com.chavez.store.api_rest.dto.response.ProductoResponseDTO;
import com.chavez.store.api_rest.dto.response.StockProductoResponseDTO;
import com.chavez.store.api_rest.dto.response.VerificarStockResponseDTO;
import com.chavez.store.api_rest.entity.Lote;
import com.chavez.store.api_rest.entity.MovimientoStock;
import com.chavez.store.api_rest.entity.Producto;
import com.chavez.store.api_rest.entity.User;
import com.chavez.store.api_rest.entity.enums.TipoMovimientoStock;
import com.chavez.store.api_rest.exception.NegativeStockException;
import com.chavez.store.api_rest.exception.ReglaNegocioException;
import com.chavez.store.api_rest.exception.ResourceNotFoundException;
import com.chavez.store.api_rest.repository.dao.ILoteDAO;
import com.chavez.store.api_rest.repository.dao.IMovimientoDAO;
import com.chavez.store.api_rest.repository.dao.IProductoDAO;
import com.chavez.store.api_rest.repository.dao.IProductoDAOExtra;
import com.chavez.store.api_rest.repository.dao.IUserDAO;
import com.chavez.store.api_rest.service.IInventarioService;
import com.chavez.store.api_rest.service.IProductoService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inventario: lotes, mermas, ajustes y verificacion del invariante.
 * Reglas R-I-01 a R-I-11 (LOGICA_NEGOCIO.md seccion 3.3).
 *
 * Principio: el kardex (stock_movements) es la fuente de verdad. products.stock
 * es un cache que se actualiza en la misma transaccion que el movimiento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventarioServiceImpl implements IInventarioService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final IProductoDAO productoDAO;
    private final IProductoDAOExtra productoDAOExtra;
    private final ILoteDAO loteDAO;
    private final IMovimientoDAO movimientoDAO;
    private final IUserDAO userDAO;
    private final IProductoService productoService;

    // ══════════════════════════════════════════════════════════
    //  MERMAS
    // ══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public KardexResponseDTO registrarMerma(MermaRequestDTO request) {
        Producto producto = productoDAO.findByIdForUpdate(request.getProductoId())
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", request.getProductoId()));

        // R-I-07: motivo obligatorio
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw ReglaNegocioException.motivoObligatorio("MERMA");
        }

        // R-I-01: nunca stock negativo
        if (producto.getStock() < request.getQty()) {
            throw new NegativeStockException(producto.getName(), producto.getStock(), request.getQty());
        }

        User usuario = usuarioActual();
        List<LoteAfectadoDTO> afectados = new ArrayList<>();
        BigDecimal costoPerdido = BigDecimal.ZERO;

        List<Lote> lotes = seleccionarLotes(producto, request);
        int disponible = lotes.stream().mapToInt(Lote::getQtyRemaining).sum();
        if (disponible < request.getQty()) {
            throw new NegativeStockException(producto.getName(), disponible, request.getQty());
        }

        int pendiente = request.getQty();
        for (Lote lote : lotes) {
            if (pendiente == 0) {
                break;
            }
            int tomar = Math.min(lote.getQtyRemaining(), pendiente);
            lote.setQtyRemaining(lote.getQtyRemaining() - tomar);
            loteDAO.save(lote);

            // El costo perdido se mide con el costo del lote, no con el promedio
            costoPerdido = costoPerdido.add(
                    BigDecimal.valueOf(tomar).multiply(lote.getCostUnit()));

            movimientoDAO.save(MovimientoStock.builder()
                    .producto(producto)
                    .lote(lote)
                    .type(TipoMovimientoStock.MERMA)
                    .qty(-tomar)
                    .unitCost(lote.getCostUnit())
                    .reason(request.getReason())
                    .usuario(usuario)
                    .refTable("mermas")
                    .refId(request.getProductoId())
                    .build());

            afectados.add(LoteAfectadoDTO.builder()
                    .loteId(lote.getId())
                    .lotCode(lote.getLotCode())
                    .qty(tomar)
                    .build());

            pendiente -= tomar;
        }

        producto.setStock(producto.getStock() - request.getQty());
        productoDAO.save(producto);

        log.info("Merma registrada: producto={} qty={} motivo={} costoPerdido={}",
                producto.getSku(), request.getQty(), request.getReason(), costoPerdido);

        return kardexDelUltimoMovimiento(producto);
    }

    /**
     * R-I-11: si se indica lote, se usa ese. Si no, se consumen por FEFO.
     * Se excluyen los lotes ya vencidos si hay alternativas vigentes.
     */
    private List<Lote> seleccionarLotes(Producto producto, MermaRequestDTO request) {
        if (request.getLoteId() != null) {
            Lote lote = loteDAO.findById(request.getLoteId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Lote", request.getLoteId()));
            if (!lote.getProducto().getId().equals(producto.getId())) {
                throw new ReglaNegocioException("El lote no pertenece al producto %s", producto.getName());
            }
            if (lote.getQtyRemaining() <= 0) {
                throw new ReglaNegocioException("El lote %s no tiene saldo", lote.getLotCode());
            }
            return List.of(lote);
        }

        List<Lote> todos = loteDAO.findLotesFEFO(producto.getId());
        if (todos.isEmpty()) {
            // Producto sin lotes: la merma descuenta directo del stock
            return List.of();
        }

        // Prioriza lotes vigentes; los vencidos quedan al final
        boolean hayVigentes = todos.stream().anyMatch(l -> !l.isVencido());
        if (hayVigentes) {
            List<Lote> vigentes = new ArrayList<>();
            List<Lote> vencidos = new ArrayList<>();
            for (Lote l : todos) {
                if (l.isVencido()) {
                    vencidos.add(l);
                } else {
                    vigentes.add(l);
                }
            }
            vigentes.addAll(vencidos);
            return vigentes;
        }
        return todos;
    }

    // ══════════════════════════════════════════════════════════
    //  AJUSTES
    // ══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public KardexResponseDTO registrarAjuste(AjusteStockRequestDTO request) {
        Producto producto = productoDAO.findByIdForUpdate(request.getProductoId())
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", request.getProductoId()));

        // R-I-06: motivo obligatorio
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw ReglaNegocioException.motivoObligatorio("AJUSTE");
        }

        int diferencia = request.getStock() - producto.getStock();
        if (diferencia == 0) {
            throw new ReglaNegocioException(
                    "El stock ya es %d: no hay diferencia que ajustar", producto.getStock());
        }

        User usuario = usuarioActual();
        TipoMovimientoStock tipo = diferencia > 0
                ? TipoMovimientoStock.AJUSTE_POSITIVO
                : TipoMovimientoStock.AJUSTE_NEGATIVO;

        // Si es negativo, se descuenta de los lotes por FEFO
        if (diferencia < 0) {
            descontarDeLotes(producto, Math.abs(diferencia));
        }

        producto.setStock(producto.getStock() + diferencia);
        productoDAO.save(producto);

        movimientoDAO.save(MovimientoStock.builder()
                .producto(producto)
                .type(tipo)
                .qty(diferencia)
                .unitCost(producto.getCostAvg())
                .reason(request.getReason())
                .usuario(usuario)
                .refTable("ajustes")
                .refId(producto.getId())
                .build());

        return kardexDelUltimoMovimiento(producto);
    }

    private void descontarDeLotes(Producto producto, int cantidad) {
        List<Lote> lotes = loteDAO.findLotesFEFO(producto.getId());
        int disponible = lotes.stream().mapToInt(Lote::getQtyRemaining).sum();
        if (disponible < cantidad) {
            throw new NegativeStockException(producto.getName(), disponible, cantidad);
        }
        int pendiente = cantidad;
        for (Lote lote : lotes) {
            if (pendiente == 0) {
                break;
            }
            int tomar = Math.min(lote.getQtyRemaining(), pendiente);
            lote.setQtyRemaining(lote.getQtyRemaining() - tomar);
            loteDAO.save(lote);
            pendiente -= tomar;
        }
    }

    // ══════════════════════════════════════════════════════════
    //  STOCK INICIAL
    // ══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public ProductoResponseDTO cargarStockInicial(Long productoId, StockInicialRequestDTO request) {
        Producto producto = productoDAO.findByIdForUpdate(productoId)
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", productoId));

        if (request.getReason() == null || request.getReason().isBlank()) {
            throw ReglaNegocioException.motivoObligatorio("INVENTARIO_INICIAL");
        }

        User usuario = usuarioActual();
        List<LoteInicialDTO> lotes = request.getLotes();

        // Sin lotes explicitos: se crea un lote unico por la cantidad total
        if (lotes == null || lotes.isEmpty()) {
            lotes = List.of(LoteInicialDTO.builder()
                    .lotCode(generarLotCode(producto.getSku(), LocalDate.now()))
                    .entryDate(LocalDate.now())
                    .qty(request.getUnitsBase())
                    .costUnit(producto.getCostAvg())
                    .build());
        }

        int total = 0;
        BigDecimal valorTotal = BigDecimal.ZERO;
        int secuencia = 0;

        for (LoteInicialDTO l : lotes) {
            Lote lote = Lote.builder()
                    .lotCode(secuencia++ == 0 ? l.getLotCode() : l.getLotCode() + "-" + secuencia)
                    .producto(producto)
                    .entryDate(l.getEntryDate() == null ? LocalDate.now() : l.getEntryDate())
                    .expiryDate(l.getExpiryDate())
                    .qtyReceived(l.getQty())
                    .qtyRemaining(l.getQty())
                    .costUnit(l.getCostUnit())
                    .active(true)
                    .build();

            // Evita colision de lot_code si el usuario los repite
            String codigo = lote.getLotCode();
            int intento = 1;
            while (loteDAO.existsByLotCode(codigo)) {
                codigo = lote.getLotCode() + "-" + (++intento);
            }
            lote.setLotCode(codigo);
            loteDAO.save(lote);

            total += l.getQty();
            valorTotal = valorTotal.add(BigDecimal.valueOf(l.getQty()).multiply(l.getCostUnit()));

            movimientoDAO.save(MovimientoStock.builder()
                    .producto(producto)
                    .lote(lote)
                    .type(TipoMovimientoStock.INVENTARIO_INICIAL)
                    .qty(l.getQty())
                    .unitCost(l.getCostUnit())
                    .reason(request.getReason())
                    .usuario(usuario)
                    .refTable("inventario_inicial")
                    .refId(productoId)
                    .build());
        }

        producto.setStock(producto.getStock() + total);
        // El costo promedio del inventario inicial es el promedio ponderado de sus lotes
        if (total > 0) {
            producto.setCostAvg(valorTotal.divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP));
        }
        productoDAO.save(producto);

        return productoService.obtener(productoId);
    }

    private String generarLotCode(String sku, LocalDate hoy) {
        String base = "LOTE-%s-%s".formatted(sku, hoy.toString().replace("-", ""));
        String codigo = "%s-%03d".formatted(base, loteDAO.count() + 1);
        int intento = 1;
        while (loteDAO.existsByLotCode(codigo)) {
            codigo = "%s-%03d".formatted(base, ++intento);
        }
        return codigo;
    }

    // ══════════════════════════════════════════════════════════
    //  CONSULTAS
    // ══════════════════════════════════════════════════════════

    /**
 * Kardex con el stock resultante por movimiento.
 * El saldo lo calcula el DAO en SQL con suma acumulada, de modo que el
 * resultado no depende del orden de lectura ni de marcas de tiempo repetidas.
 */
    @Override
    @Transactional(readOnly = true)
    public Page<KardexResponseDTO> kardex(Long productoId, Pageable pageable) {
        Producto producto = productoDAO.findById(productoId)
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", productoId));

        long total = movimientoDAO.contarPorProducto(productoId);
        List<Object[]> filas = movimientoDAO.kardexConSaldo(
                productoId, (int) pageable.getOffset(), pageable.getPageSize());

        List<KardexResponseDTO> contenido = filas.stream()
                .map(f -> toKardex(producto, f))
                .toList();

        return new org.springframework.data.domain.PageImpl<>(
                contenido, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public StockProductoResponseDTO stockDe(Long productoId) {
        Producto producto = productoDAO.findById(productoId)
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", productoId));

        List<Lote> lotes = loteDAO
                .findByProductoIdAndQtyRemainingGreaterThanOrderByExpiryDateAsc(productoId, 0);

        String estado = producto.getStock() == 0
                ? "SIN_STOCK"
                : (producto.getStock() <= producto.getMinStock() ? "CRITICO" : "NORMAL");

        BigDecimal valor = BigDecimal.valueOf(producto.getStock()).multiply(producto.getCostAvg());

        return StockProductoResponseDTO.builder()
                .productoId(productoId)
                .sku(producto.getSku())
                .nombre(producto.getName())
                .stock(producto.getStock())
                .stockMin(producto.getMinStock())
                .stockMax(producto.getMaxStock())
                .estado(estado)
                .valorInventario(valor)
                .lotes(lotes.stream().map(this::toLoteDTO).toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteStockResponseDTO> lotesPorVencer(int dias) {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(dias);
        return loteDAO.findLotesPorVencer(hoy, limite).stream().map(this::toLoteDTO).toList();
    }

    /** R-I-04: products.stock debe coincidir con la suma del kardex. */
    @Override
    @Transactional(readOnly = true)
    public VerificarStockResponseDTO verificarInvariante() {
        List<Object[]> filas = productoDAOExtra.verificarInvarianteStock();

        List<DescuadreDTO> descuadres = new ArrayList<>();
        for (Object[] fila : filas) {
            Number id = (Number) fila[0];
            String sku = (String) fila[1];
            String nombre = (String) fila[2];
            int cacheado = ((Number) fila[3]).intValue();
            int real = ((Number) fila[4]).intValue();

            descuadres.add(DescuadreDTO.builder()
                    .productoId(id.longValue())
                    .sku(sku)
                    .nombre(nombre)
                    .stockCacheado(cacheado)
                    .stockReal(real)
                    .diferencia(cacheado - real)
                    .build());
        }

        int total = (int) productoDAO.count();

        if (!descuadres.isEmpty()) {
            log.error("ALERTA DE AUDITORIA: {} productos con descuadre de inventario", descuadres.size());
        }

        return VerificarStockResponseDTO.builder()
                .verificado(descuadres.isEmpty())
                .totalProductos(total)
                .productosConDescuadre(descuadres.size())
                .descuadres(descuadres)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> stockCritico() {
        return productoDAO.findStockCritico().stream()
                .map(p -> productoService.obtener(p.getId()))
                .toList();
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════

    private User usuarioActual() {
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return userDAO.findByUsername(username)
                .orElseThrow(() -> ResourceNotFoundException.porNombre("Usuario", username));
    }

    private KardexResponseDTO kardexDelUltimoMovimiento(Producto producto) {
        List<Object[]> filas = movimientoDAO.kardexConSaldo(producto.getId(), 0, 1);
        if (filas.isEmpty()) {
            throw new ResourceNotFoundException("No se registro el movimiento");
        }
        return toKardex(producto, filas.get(0));
    }

    /** Mapea una fila del kardex nativo. */
    private KardexResponseDTO toKardex(Producto producto, Object[] f) {
        BigDecimal qty = BigDecimal.valueOf(((Number) f[1]).intValue());
        BigDecimal unitCost = toDecimal(f[2]);
        int stockResultante = ((Number) f[3]).intValue();

        return KardexResponseDTO.builder()
                .id(((Number) f[0]).longValue())
                .productoId(producto.getId())
                .productoNombre(producto.getName())
                .loteId(f[9] == null ? null : ((Number) f[9]).longValue())
                .lotCode((String) f[10])
                .type(TipoMovimientoStock.valueOf((String) f[4]))
                .qty(qty.intValue())
                .unitCost(unitCost)
                // Valor absoluto: el signo ya lo indica qty
                .valorMovimiento(qty.abs().multiply(unitCost).setScale(2, java.math.RoundingMode.HALF_UP))
                .refTable((String) f[6])
                .refId(f[7] == null ? null : ((Number) f[7]).longValue())
                .reason((String) f[5])
                .usuario((String) f[12])
                .stockResultante(stockResultante)
                .createdAt(toLocalDateTime(f[13]))
                .build();
    }

    private BigDecimal toDecimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        if (valor instanceof BigDecimal bd) {
            return bd;
        }
        if (valor instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private LocalDateTime toLocalDateTime(Object valor) {
        if (valor instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (valor instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        return LocalDateTime.now();
    }

    private LoteStockResponseDTO toLoteDTO(Lote l) {
        boolean vencido = l.isVencido();
        Long dias = l.getExpiryDate() == null
                ? null
                : ChronoUnit.DAYS.between(LocalDate.now(), l.getExpiryDate());

        return LoteStockResponseDTO.builder()
                .loteId(l.getId())
                .lotCode(l.getLotCode())
                .entryDate(l.getEntryDate())
                .expiryDate(l.getExpiryDate())
                .qtyRemaining(l.getQtyRemaining())
                .qtyReceived(l.getQtyReceived())
                .costUnit(l.getCostUnit())
                .estado(vencido ? "VENCIDO" : "VIGENTE")
                .diasRestantes(dias)
                .build();
    }
}