package com.chavez.store.api_rest.service.impl;

import com.chavez.store.api_rest.dto.request.AnularVentaRequestDTO;
import com.chavez.store.api_rest.dto.request.DetalleVentaRequestDTO;
import com.chavez.store.api_rest.dto.request.VentaRequestDTO;
import com.chavez.store.api_rest.dto.response.DetalleVentaResponseDTO;
import com.chavez.store.api_rest.dto.response.PageResponseDTO;
import com.chavez.store.api_rest.dto.response.VentaResponseDTO;
import com.chavez.store.api_rest.entity.Cliente;
import com.chavez.store.api_rest.entity.DetalleVenta;
import com.chavez.store.api_rest.entity.Lote;
import com.chavez.store.api_rest.entity.MovimientoStock;
import com.chavez.store.api_rest.entity.Presentacion;
import com.chavez.store.api_rest.entity.Producto;
import com.chavez.store.api_rest.entity.User;
import com.chavez.store.api_rest.entity.Venta;
import com.chavez.store.api_rest.entity.enums.EstadoVenta;
import com.chavez.store.api_rest.entity.enums.TipoMovimientoStock;
import com.chavez.store.api_rest.entity.enums.TipoPresentacion;
import com.chavez.store.api_rest.entity.enums.TipoVenta;
import com.chavez.store.api_rest.exception.EmptySaleException;
import com.chavez.store.api_rest.exception.InsufficientStockException;
import com.chavez.store.api_rest.exception.InvalidSaleDateException;
import com.chavez.store.api_rest.exception.NoCreditLimitException;
import com.chavez.store.api_rest.exception.PaymentLimitExceededException;
import com.chavez.store.api_rest.exception.ReglaNegocioException;
import com.chavez.store.api_rest.exception.ResourceNotFoundException;
import com.chavez.store.api_rest.exception.SaleStateException;
import com.chavez.store.api_rest.repository.dao.IClienteDAO;
import com.chavez.store.api_rest.repository.dao.ILoteDAO;
import com.chavez.store.api_rest.repository.dao.IMovimientoDAO;
import com.chavez.store.api_rest.repository.dao.IPresentacionDAO;
import com.chavez.store.api_rest.repository.dao.IProductoDAO;
import com.chavez.store.api_rest.repository.dao.IUserDAO;
import com.chavez.store.api_rest.repository.dao.IVentaDAO;
import com.chavez.store.api_rest.service.IVentaService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ventas como REGISTRO INTERNO: documentan la salida de stock y la utilidad.
 * No hay cobro, no hay voucher, no hay punto de venta al publico.
 * Reglas R-V-01 a R-V-18 (LOGICA_NEGOCIO.md seccion 3.5).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VentaServiceImpl implements IVentaService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final IVentaDAO ventaDAO;
    private final IProductoDAO productoDAO;
    private final IPresentacionDAO presentacionDAO;
    private final ILoteDAO loteDAO;
    private final IMovimientoDAO movimientoDAO;
    private final IClienteDAO clienteDAO;
    private final IUserDAO userDAO;

    @Override
    @Transactional
    public VentaResponseDTO registrar(VentaRequestDTO request) {
        // ── R-V-01: al menos un item ──
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new EmptySaleException();
        }

        // ── R-V-17: la fecha no puede ser futura ──
        LocalDateTime fechaVenta = request.getSaleDate() == null
                ? LocalDateTime.now()
                : request.getSaleDate();
        if (fechaVenta.isAfter(LocalDateTime.now())) {
            throw new InvalidSaleDateException();
        }

        TipoVenta tipo = request.getType();
        Cliente cliente = null;

        // ── R-V-13: venta a credito exige cliente con cupo ──
        if (tipo == TipoVenta.CREDITO) {
            if (request.getClienteId() == null) {
                throw new ReglaNegocioException(
                        "La venta a credito exige un cliente (R-V-13)");
            }
            cliente = clienteDAO.findById(request.getClienteId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Cliente", request.getClienteId()));

            if (!Boolean.TRUE.equals(cliente.getActive())) {
                throw ReglaNegocioException.clienteInactivo(cliente.getFullName());
            }
            if (cliente.getCreditLimit() == null
                    || cliente.getCreditLimit().compareTo(BigDecimal.ZERO) <= 0) {
                throw new NoCreditLimitException(cliente.getFullName());
            }
        }

        User usuario = usuarioActual();

        // ══════════════════════════════════════════════════════
        //  1. Preparar items: validar y calcular
        // ══════════════════════════════════════════════════════
        Map<Long, ItemPreparado> items = new HashMap<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal costoTotal = BigDecimal.ZERO;

        for (DetalleVentaRequestDTO d : request.getDetalles()) {
            // Bloqueo pesimista: evita que dos ventas concurrenten el mismo stock
            Producto producto = productoDAO.findByIdForUpdate(d.getProductoId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Producto", d.getProductoId()));

            Presentacion presentacion = presentacionDAO.findById(d.getPresentacionId())
                    .orElseThrow(() -> ResourceNotFoundException.of(
                            "Presentacion", d.getPresentacionId()));

            // ── R-V-02: producto y presentacion deben estar activos y con precio ──
            if (!Boolean.TRUE.equals(producto.getActive())) {
                throw SaleStateException.productoInactivo(producto.getName());
            }
            if (!Boolean.TRUE.equals(presentacion.getActive())) {
                throw SaleStateException.productoInactivo(
                        producto.getName() + " / " + presentacion.getName());
            }
            if (presentacion.getType() != TipoPresentacion.VENTA) {
                throw new ReglaNegocioException(
                        "La presentacion '%s' no es de venta (es de COMPRA)",
                        presentacion.getName());
            }
            if (presentacion.getPrice() == null
                    || presentacion.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw SaleStateException.productoInactivo(
                        producto.getName() + " (presentacion sin precio)");
            }

            int unidadesBase = d.getQty() * presentacion.getUnitsBase();

            // ── R-V-05: stock suficiente ──
            if (producto.getStock() < unidadesBase) {
                throw new InsufficientStockException(
                        producto.getName(), producto.getStock(), unidadesBase);
            }

            // Si el mismo producto aparece dos veces, se acumula la demanda
            ItemPreparado previo = items.get(producto.getId());
            int unidadesAcumuladas = (previo == null ? 0 : previo.unidadesBase) + unidadesBase;

            // La validacion se hace sobre el acumulado, no item por item
            if (producto.getStock() < unidadesAcumuladas) {
                throw new InsufficientStockException(
                        producto.getName(), producto.getStock(), unidadesAcumuladas);
            }

            // ── R-V-06: snapshot de precio y costo en el momento ──
            BigDecimal unitPrice = presentacion.getPrice();
            BigDecimal unitCost = producto.getCostAvg();

            items.put(producto.getId(), new ItemPreparado(
                    producto, presentacion, d.getQty(), unidadesBase, unitPrice, unitCost));

            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(d.getQty())));
            costoTotal = costoTotal.add(
                    unitCost.multiply(BigDecimal.valueOf(unidadesBase)));
        }

        // ── R-V-04: sin impuestos, total == subtotal ──
        BigDecimal total = subtotal;
        BigDecimal profit = subtotal.subtract(costoTotal);

        // ── R-CL-06: el credito no puede superar el limite ──
        if (tipo == TipoVenta.CREDITO) {
            BigDecimal saldo = clienteDAO.saldoDeudor(cliente.getId());
            BigDecimal nuevoSaldo = saldo.add(total);
            if (nuevoSaldo.compareTo(cliente.getCreditLimit()) > 0) {
                throw new PaymentLimitExceededException(
                        cliente.getFullName(), saldo, total, cliente.getCreditLimit());
            }
        }

        // ══════════════════════════════════════════════════════
        //  2. Persistir la venta
        // ══════════════════════════════════════════════════════
        Venta venta = Venta.builder()
                .document(generarDocumento(fechaVenta))
                .cliente(cliente)
                .usuario(usuario)
                .saleDate(fechaVenta)
                .type(tipo)
                .subtotal(subtotal)
                .total(total)
                .costTotal(costoTotal.setScale(2, RoundingMode.HALF_UP))
                .profit(profit.setScale(2, RoundingMode.HALF_UP))
                .status(EstadoVenta.PAGADA)
                .build();
        ventaDAO.save(venta);

        // ══════════════════════════════════════════════════════
        //  3. Consumir lotes por FEFO y descontar stock
        // ══════════════════════════════════════════════════════
        for (ItemPreparado item : items.values()) {
            consumirLotesFEFO(venta, item, usuario);
            item.producto.setStock(item.producto.getStock() - item.unidadesBase);
            productoDAO.save(item.producto);
        }

        ventaDAO.save(venta);

        log.info("Venta interna registrada: {} total={} profit={} items={}",
                venta.getDocument(), venta.getTotal(), venta.getProfit(), items.size());

        return toResponse(venta);
    }

    /**
     * R-I-10: descuenta del lote que vence primero.
     * Se crea un sale_detail por cada lote tocado, por eso el producto puede
     * tener mas de un detalle por la misma presentacion.
     */
    private void consumirLotesFEFO(Venta venta, ItemPreparado item, User usuario) {
        List<Lote> lotes = loteDAO.findLotesFEFO(item.producto.getId());
        int pendiente = item.unidadesBase;

        for (Lote lote : lotes) {
            if (pendiente == 0) {
                break;
            }
            // R-I-11: no se consume lote vencido si quedan vigentes
            if (lote.isVencido() && hayLoteVigente(lotes)) {
                continue;
            }

            int tomar = Math.min(lote.getQtyRemaining(), pendiente);
            if (tomar <= 0) {
                continue;
            }

            lote.setQtyRemaining(lote.getQtyRemaining() - tomar);
            loteDAO.save(lote);

            // El precio de la presentacion se prorratea por unidad base:
            // si un pack x6 sale 25.00, cada unidad base vale 25/6.
            BigDecimal precioPorBase = item.precioPorUnidadBase();
            BigDecimal cantidad = BigDecimal.valueOf(tomar);
            BigDecimal subtotalDetalle = precioPorBase.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            BigDecimal costoDetalle = item.unitCost.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);

            venta.agregarDetalle(DetalleVenta.builder()
                    .producto(item.producto)
                    .presentacion(item.presentacion)
                    .lote(lote)
                    .qty(tomar)
                    .unitsBase(tomar)
                    .unitPrice(item.unitPrice)
                    .unitCost(item.unitCost)
                    .subtotal(subtotalDetalle)
                    .profit(subtotalDetalle.subtract(costoDetalle))
                    .build());

            movimientoDAO.save(MovimientoStock.builder()
                    .producto(item.producto)
                    .lote(lote)
                    .presentacion(item.presentacion)
                    .type(TipoMovimientoStock.VENTA)
                    .qty(-tomar)
                    .unitCost(item.unitCost)
                    .refTable("sale_details")
                    .usuario(usuario)
                    .build());

            pendiente -= tomar;
        }

        // Sin lotes: sale directo del stock (producto que no maneja vencimiento)
        if (pendiente > 0) {
            BigDecimal cantidad = BigDecimal.valueOf(pendiente);
            BigDecimal precioPorBase = item.precioPorUnidadBase();
            BigDecimal subtotalDetalle = precioPorBase.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            BigDecimal costoDetalle = item.unitCost.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);

            venta.agregarDetalle(DetalleVenta.builder()
                    .producto(item.producto)
                    .presentacion(item.presentacion)
                    .lote(null)
                    .qty(pendiente)
                    .unitsBase(pendiente)
                    .unitPrice(item.unitPrice)
                    .unitCost(item.unitCost)
                    .subtotal(subtotalDetalle)
                    .profit(subtotalDetalle.subtract(costoDetalle))
                    .build());

            movimientoDAO.save(MovimientoStock.builder()
                    .producto(item.producto)
                    .presentacion(item.presentacion)
                    .type(TipoMovimientoStock.VENTA)
                    .qty(-pendiente)
                    .unitCost(item.unitCost)
                    .refTable("sale_details")
                    .usuario(usuario)
                    .build());
        }
    }

    private boolean hayLoteVigente(List<Lote> lotes) {
        return lotes.stream().anyMatch(l -> l.getQtyRemaining() > 0 && !l.isVencido());
    }

    /** R-V-09: anula la venta y devuelve el stock a los lotes originales. */
    @Override
    @Transactional
    public VentaResponseDTO anular(Long id, AnularVentaRequestDTO request) {
        Venta venta = ventaDAO.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Venta", id));

        if (venta.getStatus() == EstadoVenta.ANULADA) {
            throw SaleStateException.ventaYaAnulada();
        }

        User usuario = usuarioActual();

        for (DetalleVenta detalle : venta.getDetalles()) {
            Producto producto = productoDAO.findByIdForUpdate(detalle.getProducto().getId())
                    .orElseThrow(() -> ResourceNotFoundException.of(
                            "Producto", detalle.getProducto().getId()));

            // Devolver al lote original
            if (detalle.getLote() != null) {
                Lote lote = detalle.getLote();
                lote.setQtyRemaining(lote.getQtyRemaining() + detalle.getUnitsBase());
                loteDAO.save(lote);
            }

            producto.setStock(producto.getStock() + detalle.getUnitsBase());
            productoDAO.save(producto);

            // Movimiento inverso: el kardex es append-only, nunca se borra (R-I-05)
            movimientoDAO.save(MovimientoStock.builder()
                    .producto(producto)
                    .lote(detalle.getLote())
                    .presentacion(detalle.getPresentacion())
                    .type(TipoMovimientoStock.VENTA)
                    .qty(detalle.getUnitsBase())
                    .unitCost(detalle.getUnitCost())
                    .reason("Anulacion venta " + venta.getDocument())
                    .refTable("sale_details")
                    .refId(detalle.getId())
                    .usuario(usuario)
                    .build());
        }

        venta.setStatus(EstadoVenta.ANULADA);
        venta.setAnnulledAt(LocalDateTime.now());
        venta.setAnnulReason(request.getReason());
        venta.setAnnulUser(usuario);
        ventaDAO.save(venta);

        log.info("Venta anulada: {} motivo={}", venta.getDocument(), request.getReason());

        return toResponse(venta);
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponseDTO obtener(Long id) {
        return toResponse(ventaDAO.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Venta", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<VentaResponseDTO> buscar(EstadoVenta status, TipoVenta tipo, Long clienteId,
                                                    LocalDateTime desde, LocalDateTime hasta,
                                                    int page, int size) {
        return PageResponseDTO.of(ventaDAO.buscar(status, tipo, clienteId, desde, hasta,
                        PageRequest.of(page, size, Sort.by("saleDate").descending()))
                .map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponseDTO> listarPorCliente(Long clienteId) {
        return ventaDAO.findByClienteIdAndTypeAndStatusOrderBySaleDateDesc(
                clienteId, TipoVenta.CREDITO, EstadoVenta.PAGADA).stream()
                .map(this::toResponse)
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

    /** Correlativo por anio: V-2026-000001 */
    private String generarDocumento(LocalDateTime fecha) {
        String base = "V-%d".formatted(fecha.getYear());
        long correlativo = ventaDAO.count() + 1;
        String documento = "%s-%06d".formatted(base, correlativo);
        int intento = 0;
        while (ventaDAO.existsByDocument(documento)) {
            documento = "%s-%06d".formatted(base, ++correlativo + intento++);
        }
        return documento;
    }

    private VentaResponseDTO toResponse(Venta v) {
        List<DetalleVentaResponseDTO> detalles = new ArrayList<>();
        for (DetalleVenta d : v.getDetalles()) {
            detalles.add(DetalleVentaResponseDTO.builder()
                    .id(d.getId())
                    .productoId(d.getProducto().getId())
                    .productoNombre(d.getProducto().getName())
                    .productoSku(d.getProducto().getSku())
                    .presentacionId(d.getPresentacion().getId())
                    .presentacionNombre(d.getPresentacion().getName())
                    .loteId(d.getLote() == null ? null : d.getLote().getId())
                    .lotCode(d.getLote() == null ? null : d.getLote().getLotCode())
                    .expiryDate(d.getLote() == null ? null : d.getLote().getExpiryDate())
                    .qty(d.getQty())
                    .unitsBase(d.getUnitsBase())
                    .unitPrice(d.getUnitPrice())
                    .unitCost(d.getUnitCost())
                    .subtotal(d.getSubtotal())
                    .profit(d.getProfit())
                    .build());
        }

        BigDecimal margen = BigDecimal.ZERO;
        if (v.getTotal() != null && v.getTotal().compareTo(BigDecimal.ZERO) > 0) {
            margen = v.getProfit().divide(v.getTotal(), 6, RoundingMode.HALF_UP)
                    .multiply(CIEN).setScale(2, RoundingMode.HALF_UP);
        }

        return VentaResponseDTO.builder()
                .id(v.getId())
                .document(v.getDocument())
                .type(v.getType())
                .clienteId(v.getCliente() == null ? null : v.getCliente().getId())
                .clienteNombre(v.getCliente() == null ? null : v.getCliente().getFullName())
                .registradoPor(v.getUsuario() == null ? null : v.getUsuario().getUsername())
                .fecha(v.getSaleDate())
                .subtotal(v.getSubtotal())
                .total(v.getTotal())
                .costTotal(v.getCostTotal())
                .profit(v.getProfit())
                .margenPct(margen)
                .status(v.getStatus())
                .annulledAt(v.getAnnulledAt())
                .annulReason(v.getAnnulReason())
                .detalles(detalles)
                .build();
    }

    /** Item preparado con su snapshot de precio y costo. */
    private record ItemPreparado(
            Producto producto,
            Presentacion presentacion,
            int qty,
            int unidadesBase,
            BigDecimal unitPrice,
            BigDecimal unitCost) {

        /**
 * Precio de una sola unidad base.
 *
 * El precio de la presentacion corresponde a una presentacion completa
 * (Pack x6 = S/25 las 6 botellas), no a una unidad base. Por eso se divide
 * entre las unidades base DE LA PRESENTACION, no entre las de la linea:
 *
 *   Pack x6 a S/25 -> 25 / 6 = 4.1667 por botella
 *   Venta de 3 packs = 18 botellas -> 18 x 4.1667 = S/75
 */
        BigDecimal precioPorUnidadBase() {
            int factor = presentacion == null ? 1 : presentacion.getUnitsBase();
            if (factor <= 0) {
                return BigDecimal.ZERO;
            }
            return unitPrice.divide(BigDecimal.valueOf(factor), 6, RoundingMode.HALF_UP);
        }
    }
}