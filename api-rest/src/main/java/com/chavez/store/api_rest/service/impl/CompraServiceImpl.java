package com.chavez.store.api_rest.service.impl;

import com.chavez.store.api_rest.dto.request.CompraRequestDTO;
import com.chavez.store.api_rest.dto.request.DetalleCompraRequestDTO;
import com.chavez.store.api_rest.dto.response.CompraResponseDTO;
import com.chavez.store.api_rest.dto.response.DetalleCompraResponseDTO;
import com.chavez.store.api_rest.dto.response.PageResponseDTO;
import com.chavez.store.api_rest.entity.Compra;
import com.chavez.store.api_rest.entity.DetalleCompra;
import com.chavez.store.api_rest.entity.Lote;
import com.chavez.store.api_rest.entity.MovimientoStock;
import com.chavez.store.api_rest.entity.Presentacion;
import com.chavez.store.api_rest.entity.Producto;
import com.chavez.store.api_rest.entity.Proveedor;
import com.chavez.store.api_rest.entity.User;
import com.chavez.store.api_rest.entity.enums.EstadoCompra;
import com.chavez.store.api_rest.entity.enums.TipoMovimientoStock;
import com.chavez.store.api_rest.entity.enums.TipoPresentacion;
import com.chavez.store.api_rest.exception.NegativeStockException;
import com.chavez.store.api_rest.exception.PurchaseStateException;
import com.chavez.store.api_rest.exception.ReglaNegocioException;
import com.chavez.store.api_rest.exception.ResourceNotFoundException;
import com.chavez.store.api_rest.repository.dao.ICompraDAO;
import com.chavez.store.api_rest.repository.dao.ILoteDAO;
import com.chavez.store.api_rest.repository.dao.IMovimientoDAO;
import com.chavez.store.api_rest.repository.dao.IPresentacionDAO;
import com.chavez.store.api_rest.repository.dao.IProductoDAO;
import com.chavez.store.api_rest.repository.dao.IProveedorDAO;
import com.chavez.store.api_rest.repository.dao.IUserDAO;
import com.chavez.store.api_rest.service.ICompraService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compras e ingreso a almacen.
 * Reglas R-CO-01 a R-CO-09 (LOGICA_NEGOCIO.md seccion 3.4).
 *
 * Recibir una compra hace tres cosas a la vez:
 *   1. crea un lote por item (para controlar vencimiento y salida FEFO),
 *   2. suma stock al producto en UNIDAD BASE,
 *   3. recalcula el costo promedio ponderado movil.
 */
@Service
@RequiredArgsConstructor
public class CompraServiceImpl implements ICompraService {

    private final ICompraDAO compraDAO;
    private final IProveedorDAO proveedorDAO;
    private final IProductoDAO productoDAO;
    private final IPresentacionDAO presentacionDAO;
    private final ILoteDAO loteDAO;
    private final IMovimientoDAO movimientoDAO;
    private final IUserDAO userDAO;

    @Override
    @Transactional
    public CompraResponseDTO registrar(CompraRequestDTO request) {
        // R-CO-01: al menos un item
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw ReglaNegocioException.compraVacia();
        }

        Proveedor proveedor = proveedorDAO.findById(request.getProveedorId())
                .orElseThrow(() -> ResourceNotFoundException.of("Proveedor", request.getProveedorId()));

        // R-CO-09: proveedor inactivo no recibe compras nuevas
        if (!Boolean.TRUE.equals(proveedor.getActive())) {
            throw ReglaNegocioException.proveedorInactivo(proveedor.getName());
        }

        // R-CO-06: documento unico; si no viene, autogenerado
        String documento = request.getDocument();
        if (documento == null || documento.isBlank()) {
            documento = "COM-%d-%06d".formatted(request.getIssueDate().getYear(), compraDAO.count() + 1);
        } else if (compraDAO.existsByDocument(documento)) {
            throw new ReglaNegocioException("El documento de compra ya existe: " + documento);
        }

        Compra compra = Compra.builder()
                .proveedor(proveedor)
                .document(documento)
                .issueDate(request.getIssueDate())
                .status(EstadoCompra.REGISTRADA)
                .total(BigDecimal.ZERO)
                .notes(request.getNotes())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (DetalleCompraRequestDTO d : request.getDetalles()) {
            Producto producto = productoDAO.findById(d.getProductoId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Producto", d.getProductoId()));

            Presentacion presentacion = presentacionDAO.findById(d.getPresentacionId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Presentacion", d.getPresentacionId()));

            // R-CO-08: producto y presentacion deben estar activos
            if (!Boolean.TRUE.equals(producto.getActive())) {
                throw new ReglaNegocioException(
                        "El producto esta inactivo: " + producto.getName());
            }
            if (!Boolean.TRUE.equals(presentacion.getActive())) {
                throw new ReglaNegocioException(
                        "La presentacion esta inactiva: " + presentacion.getName());
            }

            // La presentacion de compra convierte a unidad base
            int unidadesBase = d.getQty() * presentacion.getUnitsBase();
            BigDecimal subtotal = BigDecimal.valueOf(d.getQty()).multiply(d.getUnitCost());

            DetalleCompra detalle = DetalleCompra.builder()
                    .compra(compra)
                    .producto(producto)
                    .presentacion(presentacion)
                    .qtyBought(d.getQty())
                    .qtyReceived(0)
                    .unitsBase(unidadesBase)
                    .unitCost(d.getUnitCost())
                    .subtotal(subtotal)
                    .expiryDate(d.getExpiryDate())
                    .build();

            compra.agregarDetalle(detalle);
            total = total.add(subtotal);
        }

        // R-CO-07: el total se calcula en el servidor
        compra.setTotal(total);
        compraDAO.save(compra);

        return toResponse(compra);
    }

    /**
     * R-CO-03: recibe la compra. Crea lotes, suma stock y recalcula costo promedio.
     */
    @Override
    @Transactional
    public CompraResponseDTO recibir(Long id) {
        Compra compra = obtenerEntidad(id);

        if (compra.getStatus() != EstadoCompra.REGISTRADA) {
            throw PurchaseStateException.noRegistrada();
        }

        User usuario = usuarioActual();
        LocalDate hoy = LocalDate.now();

        for (DetalleCompra detalle : compra.getDetalles()) {
            Producto producto = productoDAO.findByIdForUpdate(detalle.getProducto().getId())
                    .orElseThrow(() -> ResourceNotFoundException.of(
                            "Producto", detalle.getProducto().getId()));

            // El costo del detalle es por CAJA (por presentacion de compra).
            // El costo promedio del producto y el del lote son por UNIDAD BASE.
            BigDecimal costoPorBase = costoPorUnidadBase(detalle);

            // ── 1. Crear el lote ──
            String lotCode = generarLotCode(producto.getSku(), hoy);

            Lote lote = Lote.builder()
                    .lotCode(lotCode)
                    .producto(producto)
                    .detalleCompra(detalle)
                    .entryDate(hoy)
                    .expiryDate(detalle.getExpiryDate())
                    .qtyReceived(detalle.getUnitsBase())
                    .qtyRemaining(detalle.getUnitsBase())
                    .costUnit(costoPorBase)
                    .active(true)
                    .build();
            loteDAO.save(lote);

            // ── 2. Recalcular costo promedio ponderado ANTES de sumar stock ──
            recalcularCostoPromedio(producto, detalle.getUnitsBase(), costoPorBase);

            // ── 3. Sumar stock en unidad base ──
            int stockAnterior = producto.getStock();
            producto.setStock(stockAnterior + detalle.getUnitsBase());
            productoDAO.save(producto);

            // ── 4. Movimiento de kardex ──
            MovimientoStock movimiento = MovimientoStock.builder()
                    .producto(producto)
                    .lote(lote)
                    .presentacion(detalle.getPresentacion())
                    .type(TipoMovimientoStock.COMPRA)
                    .qty(detalle.getUnitsBase())
                    .unitCost(costoPorBase)
                    .refTable("purchase_details")
                    .refId(detalle.getId())
                    .usuario(usuario)
                    .reason("Compra " + compra.getDocument())
                    .build();
            movimientoDAO.save(movimiento);

            detalle.setQtyReceived(detalle.getUnitsBase());
        }

        compra.setStatus(EstadoCompra.RECIBIDA);
        compraDAO.save(compra);

        return toResponse(compra);
    }

    /**
     * Convierte el costo por presentacion de compra a costo por unidad base.
     * Cerveza en caja x24 a 82.00 -> 82.00 / 24 = 3.4167 por botella.
     */
    private BigDecimal costoPorUnidadBase(DetalleCompra detalle) {
        int factor = detalle.getPresentacion().getUnitsBase();
        if (factor <= 0) {
            return detalle.getUnitCost();
        }
        return detalle.getUnitCost()
                .divide(BigDecimal.valueOf(factor), 4, RoundingMode.HALF_UP);
    }

    /** Lot code unico y legible: LOTE-{SKU}-{AAAAmmdd}-{consecutivo}. */
    private String generarLotCode(String sku, LocalDate hoy) {
        String base = "LOTE-%s-%s".formatted(sku, hoy.toString().replace("-", ""));
        String codigo = "%s-%03d".formatted(base, loteDAO.count() + 1);
        int intento = 1;
        while (loteDAO.existsByLotCode(codigo)) {
            codigo = "%s-%03d".formatted(base, ++intento);
        }
        return codigo;
    }

    /**
     * Costo promedio ponderado movil (LOGICA_NEGOCIO.md seccion 2.4).
     *
     *   costo_nuevo = (stock_actual x costo_actual + qty_recibida x costo_compra)
     *                 / (stock_actual + qty_recibida)
     *
     * Se calcula ANTES de sumar el stock nuevo, por eso recibe stock y cantidad por separado.
     */
    private void recalcularCostoPromedio(Producto producto, int cantidadRecibida, BigDecimal costoCompra) {
        int stockActual = producto.getStock();
        BigDecimal costoActual = producto.getCostAvg();

        if (stockActual <= 0) {
            // Primer ingreso: el costo promedio es el costo de esta compra
            producto.setCostAvg(costoCompra);
            return;
        }

        BigDecimal valorActual = BigDecimal.valueOf(stockActual).multiply(costoActual);
        BigDecimal valorCompra = BigDecimal.valueOf(cantidadRecibida).multiply(costoCompra);
        BigDecimal stockTotal = BigDecimal.valueOf(stockActual + cantidadRecibida);

        producto.setCostAvg(valorActual.add(valorCompra).divide(stockTotal, 4, RoundingMode.HALF_UP));
    }

    private LocalDate vencimientoDe(DetalleCompra detalle) {
        return detalle.getExpiryDate();
    }

    /**
     * R-CO-05: anula la compra. Devuelve el stock a los lotes originales
     * y recalcula el costo promedio.
     */
    @Override
    @Transactional
    public CompraResponseDTO anular(Long id) {
        Compra compra = obtenerEntidad(id);

        if (compra.getStatus() == EstadoCompra.ANULADA) {
            throw new PurchaseStateException("La compra ya esta ANULADA");
        }

        if (compra.getStatus() == EstadoCompra.RECIBIDA) {
            User usuario = usuarioActual();

            for (DetalleCompra detalle : compra.getDetalles()) {
                Producto producto = productoDAO.findByIdForUpdate(detalle.getProducto().getId())
                        .orElseThrow(() -> ResourceNotFoundException.of(
                                "Producto", detalle.getProducto().getId()));

                BigDecimal costoPorBase = costoPorUnidadBase(detalle);

                // Devolver las unidades al lote original
                loteDAO.findByDetalleCompraId(detalle.getId()).ifPresent(lote -> {
                    lote.setQtyRemaining(lote.getQtyRemaining() + detalle.getUnitsBase());
                    loteDAO.save(lote);
                });

                // Recalcular costo promedio sin la mercancia devuelta
                int stockActual = producto.getStock();
                int stockResultante = stockActual - detalle.getUnitsBase();
                if (stockResultante < 0) {
                    throw new NegativeStockException(producto.getName());
                }

                if (stockResultante == 0) {
                    producto.setCostAvg(BigDecimal.ZERO);
                } else {
                    BigDecimal valorTotal = BigDecimal.valueOf(stockActual).multiply(producto.getCostAvg());
                    BigDecimal valorDevuelto = BigDecimal.valueOf(detalle.getUnitsBase())
                            .multiply(costoPorBase);
                    producto.setCostAvg(
                            valorTotal.subtract(valorDevuelto).divide(
                                    BigDecimal.valueOf(stockResultante), 4, RoundingMode.HALF_UP));
                }

                producto.setStock(stockResultante);
                productoDAO.save(producto);

                MovimientoStock movimiento = MovimientoStock.builder()
                        .producto(producto)
                        .presentacion(detalle.getPresentacion())
                        .type(TipoMovimientoStock.DEVOLUCION_PROVEEDOR)
                        .qty(-detalle.getUnitsBase())
                        .unitCost(costoPorBase)
                        .refTable("purchase_details")
                        .refId(detalle.getId())
                        .usuario(usuario)
                        .reason("Anulacion compra " + compra.getDocument())
                        .build();
                movimientoDAO.save(movimiento);
            }
        }

        compra.setStatus(EstadoCompra.ANULADA);
        compraDAO.save(compra);
        return toResponse(compra);
    }

    @Override
    @Transactional(readOnly = true)
    public CompraResponseDTO obtener(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<CompraResponseDTO> listar(EstadoCompra status, int page, int size) {
        var resultado = compraDAO.findByStatusOrderByIssueDateDescIdDesc(
                status, PageRequest.of(page, size, Sort.by("issueDate").descending()));
        return PageResponseDTO.of(resultado.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraResponseDTO> listarPorProveedor(Long proveedorId, EstadoCompra status) {
        return compraDAO.findByProveedorIdAndStatusOrderByIssueDateDesc(proveedorId, status).stream()
                .map(this::toResponse)
                .toList();
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════

    private Compra obtenerEntidad(Long id) {
        return compraDAO.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Compra", id));
    }

    private User usuarioActual() {
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return userDAO.findByUsername(username)
                .orElseThrow(() -> ResourceNotFoundException.porNombre("Usuario", username));
    }

    private DetalleCompraResponseDTO toResponseDetalle(DetalleCompra d) {
        Lote lote = loteDAO.findByDetalleCompraId(d.getId()).orElse(null);
        return DetalleCompraResponseDTO.builder()
                .id(d.getId())
                .productoId(d.getProducto().getId())
                .productoNombre(d.getProducto().getName())
                .productoSku(d.getProducto().getSku())
                .presentacionId(d.getPresentacion().getId())
                .presentacionNombre(d.getPresentacion().getName())
                .unitsBasePresentacion(d.getPresentacion().getUnitsBase())
                .qtyBought(d.getQtyBought())
                .qtyReceived(d.getQtyReceived())
                .unitsBase(d.getUnitsBase())
                .unitCost(d.getUnitCost())
                .costPorUnidadBase(costoPorUnidadBase(d))
                .subtotal(d.getSubtotal())
                .expiryDate(d.getExpiryDate())
                .loteId(lote == null ? null : lote.getId())
                .lotCode(lote == null ? null : lote.getLotCode())
                .build();
    }

    CompraResponseDTO toResponse(Compra c) {
        List<DetalleCompraResponseDTO> detalles = new ArrayList<>();
        for (DetalleCompra d : c.getDetalles()) {
            detalles.add(toResponseDetalle(d));
        }
        return CompraResponseDTO.builder()
                .id(c.getId())
                .document(c.getDocument())
                .proveedorId(c.getProveedor().getId())
                .proveedorNombre(c.getProveedor().getName())
                .issueDate(c.getIssueDate())
                .status(c.getStatus())
                .total(c.getTotal())
                .notes(c.getNotes())
                .detalles(detalles)
                .build();
    }
}