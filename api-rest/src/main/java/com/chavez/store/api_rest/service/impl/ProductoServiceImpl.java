package com.chavez.store.api_rest.service.impl;

import com.chavez.store.api_rest.dto.request.PresentacionRequestDTO;
import com.chavez.store.api_rest.dto.request.PresentacionUpdateRequestDTO;
import com.chavez.store.api_rest.dto.request.ProductoRequestDTO;
import com.chavez.store.api_rest.dto.request.ProductoUpdateRequestDTO;
import com.chavez.store.api_rest.dto.response.PageResponseDTO;
import com.chavez.store.api_rest.dto.response.PresentacionResponseDTO;
import com.chavez.store.api_rest.dto.response.ProductoResponseDTO;
import com.chavez.store.api_rest.entity.Categoria;
import com.chavez.store.api_rest.entity.Lote;
import com.chavez.store.api_rest.entity.Marca;
import com.chavez.store.api_rest.entity.MovimientoStock;
import com.chavez.store.api_rest.entity.Presentacion;
import com.chavez.store.api_rest.entity.Producto;
import com.chavez.store.api_rest.entity.User;
import com.chavez.store.api_rest.entity.enums.TipoMovimientoStock;
import com.chavez.store.api_rest.entity.enums.TipoPresentacion;
import com.chavez.store.api_rest.exception.DuplicateSkuException;
import com.chavez.store.api_rest.exception.InventoryMismatchException;
import com.chavez.store.api_rest.exception.NegativeStockException;
import com.chavez.store.api_rest.exception.ProductInUseException;
import com.chavez.store.api_rest.exception.ReglaNegocioException;
import com.chavez.store.api_rest.exception.ResourceNotFoundException;
import com.chavez.store.api_rest.repository.dao.ICategoriaDAO;
import com.chavez.store.api_rest.repository.dao.ILoteDAO;
import com.chavez.store.api_rest.repository.dao.IMarcaDAO;
import com.chavez.store.api_rest.repository.dao.IMovimientoDAO;
import com.chavez.store.api_rest.repository.dao.IProductoDAO;
import com.chavez.store.api_rest.repository.dao.IPresentacionDAO;
import com.chavez.store.api_rest.repository.dao.IUserDAO;
import com.chavez.store.api_rest.service.IProductoService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catalogo: productos, presentaciones y su factor de conversion a unidad base.
 * Reglas R-C-01 a R-C-09 (LOGICA_NEGOCIO.md seccion 3.2).
 */
@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements IProductoService {

    private final IProductoDAO productoDAO;
    private final IPresentacionDAO presentacionDAO;
    private final ICategoriaDAO categoriaDAO;
    private final IMarcaDAO marcaDAO;
    private final ILoteDAO loteDAO;
    private final IMovimientoDAO movimientoDAO;
    private final IUserDAO userDAO;

    // ══════════════════════════════════════════════════════════
    //  PRODUCTOS
    // ══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public ProductoResponseDTO crear(ProductoRequestDTO request) {
        // R-C-01: SKU unico; si no viene, se autogenera
        String sku = normalizar(request.getSku());
        if (sku == null) {
            sku = generarSku();
        } else if (productoDAO.existsBySku(sku)) {
            throw new DuplicateSkuException(sku);
        }

        // R-C-02: nombre obligatorio (validado por Bean Validation), unico por categoria
        if (existeMismoNombreEnCategoria(request.getName(), request.getCategoriaId(), null)) {
            throw new ReglaNegocioException(
                    "Ya existe un producto llamado '%s' en esa categoria (R-C-02)",
                    request.getName());
        }

        // R-C-03: codigo de barras unico si viene
        String barcode = normalizar(request.getBarcode());
        if (barcode != null && productoDAO.existsByBarcode(barcode)) {
            throw new ReglaNegocioException("El codigo de barras ya existe: " + barcode);
        }

        Categoria categoria = categoriaDAO.findById(request.getCategoriaId())
                .orElseThrow(() -> ResourceNotFoundException.of("Categoria", request.getCategoriaId()));

        Marca marca = null;
        if (request.getMarcaId() != null) {
            marca = marcaDAO.findById(request.getMarcaId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Marca", request.getMarcaId()));
        }

        // R-C-09: el costo nunca se edita al crear; lo calcula el sistema al comprar
        Producto producto = Producto.builder()
                .sku(sku)
                .barcode(barcode)
                .name(request.getName().trim())
                .description(request.getDescription())
                .categoria(categoria)
                .marca(marca)
                .baseUnit(request.getBaseUnit())
                .contentMl(request.getContentMl())
                .minStock(request.getMinStock())
                .maxStock(request.getMaxStock())
                .stock(0)
                .costAvg(BigDecimal.ZERO)
                .active(true)
                .build();

        productoDAO.save(producto);

        // R-C-04: al menos una presentacion VENTA
        validarPresentaciones(producto.getId(), request.getPresentaciones());

        List<Presentacion> presentaciones = new ArrayList<>();
        int orden = 0;
        for (PresentacionRequestDTO p : request.getPresentaciones()) {
            presentaciones.add(construirPresentacion(producto, p, orden++));
        }
        presentacionDAO.saveAll(presentaciones);
        producto.setPresentaciones(presentaciones);

        return toResponse(producto);
    }

    @Override
    @Transactional
    public ProductoResponseDTO actualizar(Long id, ProductoUpdateRequestDTO request) {
        Producto producto = obtenerEntidad(id);

        producto.setName(request.getName().trim());
        producto.setDescription(request.getDescription());
        producto.setMinStock(request.getMinStock());
        producto.setMaxStock(request.getMaxStock());

        if (request.getMarcaId() != null) {
            producto.setMarca(marcaDAO.findById(request.getMarcaId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Marca", request.getMarcaId())));
        }
        if (request.getContentMl() != null) {
            producto.setContentMl(request.getContentMl());
        }
        if (request.getActive() != null) {
            producto.setActive(request.getActive());
        }

        // R-C-08: la categoria no se toca si ya tiene movimientos
        productoDAO.save(producto);
        return toResponse(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public void desactivar(Long id) {
        Producto producto = obtenerEntidad(id);

        // R-C-07: si tiene movimientos o ventas, se desactiva en vez de eliminarse
        boolean tieneMovimientos = movimientoDAO.countByProductoId(id) > 0;
        if (tieneMovimientos) {
            producto.setActive(false);
            productoDAO.save(producto);
            return;
        }

        presentacionDAO.deleteByProductoId(id);
        productoDAO.delete(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponseDTO obtener(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponseDTO obtenerPorSku(String sku) {
        Producto producto = productoDAO.findBySku(sku)
                .orElseThrow(() -> ResourceNotFoundException.porNombre("Producto", sku));
        return toResponse(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> listarTodos() {
        return productoDAO.findByActiveTrueOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ProductoResponseDTO> buscar(String search, Long categoriaId,
                                                      Long marcaId, Boolean stockBajo,
                                                      int page, int size) {
        Page<Producto> resultado = productoDAO.buscar(
                normalizar(search), categoriaId, marcaId, stockBajo,
                PageRequest.of(page, size, Sort.by("name").ascending()));

        PageResponseDTO<ProductoResponseDTO> respuesta = PageResponseDTO.of(
                resultado.map(this::toResponse));
        return respuesta;
    }

    // ══════════════════════════════════════════════════════════
    //  PRESENTACIONES
    // ══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<PresentacionResponseDTO> listarPresentaciones(Long productoId) {
        obtenerEntidad(productoId);
        return presentacionDAO.findByProductoIdOrderBySortOrderAscIdAsc(productoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PresentacionResponseDTO agregarPresentacion(Long productoId, PresentacionRequestDTO request) {
        Producto producto = obtenerEntidad(productoId);
        validarPresentacionIndividual(request.getName(), request.getUnitsBase(), request.getType(), request.getPrice());

        boolean duplicada = producto.getPresentaciones().stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(request.getName())
                        && p.getType() == request.getType());
        if (duplicada) {
            throw new ReglaNegocioException(
                    "El producto ya tiene una presentacion %s llamada '%s'",
                    request.getType(), request.getName());
        }

        int orden = producto.getPresentaciones().stream()
                .mapToInt(p -> p.getSortOrder() == null ? 0 : p.getSortOrder())
                .max().orElse(-1) + 1;

        Presentacion presentacion = construirPresentacion(producto, request, orden);
        presentacionDAO.save(presentacion);
        producto.getPresentaciones().add(presentacion);

        return toResponse(presentacion);
    }

    @Override
    @Transactional
    public PresentacionResponseDTO actualizarPresentacion(Long presentacionId,
                                                         PresentacionUpdateRequestDTO request) {
        Presentacion presentacion = presentacionDAO.findById(presentacionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Presentacion", presentacionId));

        validarPresentacionIndividual(request.getName(), request.getUnitsBase(),
                request.getType(), request.getPrice());

        // R-C-08: no se cambia el factor si ya hay movimientos con esa presentacion
        boolean cambioFactor = !presentacion.getUnitsBase().equals(request.getUnitsBase());
        if (cambioFactor && tieneMovimientosConPresentacion(presentacion)) {
            throw new ReglaNegocioException(
                    "No se puede cambiar el factor de conversion: la presentacion ya tiene "
                            + "movimientos en el kardex (R-C-08)");
        }

        presentacion.setName(request.getName());
        presentacion.setUnitsBase(request.getUnitsBase());
        presentacion.setPrice(request.getPrice() == null ? presentacion.getPrice() : request.getPrice());
        if (request.getStockMin() != null) {
            presentacion.setStockMin(request.getStockMin());
        }
        if (request.getSortOrder() != null) {
            presentacion.setSortOrder(request.getSortOrder());
        }
        if (request.getActive() != null) {
            presentacion.setActive(request.getActive());
        }

        presentacionDAO.save(presentacion);
        return toResponse(presentacion);
    }

    @Override
    @Transactional
    public void desactivarPresentacion(Long presentacionId) {
        Presentacion presentacion = presentacionDAO.findById(presentacionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Presentacion", presentacionId));
        presentacion.setActive(false);
        presentacionDAO.save(presentacion);
    }

    // ══════════════════════════════════════════════════════════
    //  INVENTARIO
    // ══════════════════════════════════════════════════════════

    /**
     * Registra un ajuste de inventario con su movimiento en el kardex (R-I-02).
     * La diferencia se descuenta de los lotes por FEFO.
     */
    @Override
    @Transactional
    public ProductoResponseDTO registrarMovimiento(Long productoId, Integer cantidad,
                                                   TipoMovimientoStock tipo, String motivo) {
        Producto producto = productoDAO.findByIdForUpdate(productoId)
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", productoId));

        // R-I-06: ajustes exigen motivo
        if (tipo.exigeMotivo() && (motivo == null || motivo.isBlank())) {
            throw ReglaNegocioException.motivoObligatorio(tipo.name());
        }

        int diferencia = tipo.getSigno().intValue() * cantidad;

        // R-I-01: nunca stock negativo
        if (producto.getStock() + diferencia < 0) {
            throw new NegativeStockException(producto.getName(), producto.getStock(), cantidad);
        }

        User usuario = usuarioActual();

        // El ajuste descuenta de los lotes por FEFO; los lotes sin saldo se ignoran
        List<Lote> lotes = loteDAO.findLotesFEFO(productoId);
        int restante = Math.abs(diferencia);
        for (Lote lote : lotes) {
            if (restante == 0) {
                break;
            }
            int disponible = lote.getQtyRemaining();
            int tomar = Math.min(disponible, restante);
            lote.setQtyRemaining(disponible - tomar);
            loteDAO.save(lote);
            restante -= tomar;
        }

        producto.setStock(producto.getStock() + diferencia);

        MovimientoStock movimiento = MovimientoStock.builder()
                .producto(producto)
                .type(tipo)
                .qty(diferencia)
                .unitCost(producto.getCostAvg())
                .reason(motivo)
                .usuario(usuario)
                .refTable("productos")
                .refId(productoId)
                .build();
        movimientoDAO.save(movimiento);

        productoDAO.save(producto);

        // R-I-09: alerta si queda por debajo del minimo
        if (producto.getStock() <= producto.getMinStock()) {
            // No es un error: solo se deja registro en el log para el reporte
            org.slf4j.LoggerFactory.getLogger(ProductoServiceImpl.class)
                    .warn("Stock de {} en {} (minimo {})",
                            producto.getName(), producto.getStock(), producto.getMinStock());
        }

        return toResponse(producto);
    }

    /** R-I-04: products.stock debe coincidir con la suma de stock_movements. */
    @Override
    @Transactional(readOnly = true)
    public ProductoResponseDTO verificarInvariante(Long productoId) {
        Producto producto = obtenerEntidad(productoId);
        long real = movimientoDAO.stockRealDe(productoId);
        if (real != producto.getStock()) {
            throw new InventoryMismatchException(producto.getSku(), producto.getStock(), (int) real);
        }
        return toResponse(producto);
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════

    private Producto obtenerEntidad(Long id) {
        return productoDAO.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", id));
    }

    private User usuarioActual() {
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return userDAO.findByUsername(username)
                .orElseThrow(() -> ResourceNotFoundException.porNombre("Usuario", username));
    }

    private void validarPresentaciones(Long productoId, List<PresentacionRequestDTO> presentaciones) {
        // R-C-04: al menos una de tipo VENTA
        boolean hayVenta = presentaciones.stream()
                .anyMatch(p -> p.getType() == TipoPresentacion.VENTA);
        if (!hayVenta) {
            throw ReglaNegocioException.sinPresentacionVenta(productoId);
        }
        for (PresentacionRequestDTO p : presentaciones) {
            validarPresentacionIndividual(p.getName(), p.getUnitsBase(), p.getType(), p.getPrice());
        }
    }

    private void validarPresentacionIndividual(String nombre, Integer unitsBase,
                                               TipoPresentacion tipo, BigDecimal precio) {
        // R-C-05: el factor de conversion debe ser positivo
        if (unitsBase == null || unitsBase < 1) {
            throw ReglaNegocioException.unitsBaseInvalido(nombre);
        }
        // R-C-06: las presentaciones de VENTA requieren precio > 0
        if (tipo == TipoPresentacion.VENTA && (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0)) {
            throw ReglaNegocioException.presentacionVentaSinPrecio(nombre);
        }
    }

    private Presentacion construirPresentacion(Producto producto, PresentacionRequestDTO p, int orden) {
        return Presentacion.builder()
                .producto(producto)
                .name(p.getName().trim())
                .unitsBase(p.getUnitsBase())
                .type(p.getType())
                .price(p.getPrice() == null ? BigDecimal.ZERO : p.getPrice())
                .stockMin(p.getStockMin())
                .sortOrder(p.getSortOrder() == null ? orden : p.getSortOrder())
                .active(true)
                .build();
    }

    private boolean tieneMovimientosConPresentacion(Presentacion presentacion) {
        return movimientoDAO.findByRefTableAndRefId("sale_details", presentacion.getId()).stream()
                .findAny().isPresent();
    }

    private boolean existeMismoNombreEnCategoria(String nombre, Long categoriaId, Long excluirId) {
        return productoDAO.findByActiveTrueOrderByNameAsc().stream()
                .filter(p -> excluirId == null || !p.getId().equals(excluirId))
                .anyMatch(p -> p.getName().equalsIgnoreCase(nombre.trim())
                        && p.getCategoria().getId().equals(categoriaId));
    }

    private String generarSku() {
        return "BEB-%06d".formatted(productoDAO.count() + 1);
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String v = valor.trim();
        return v.isEmpty() ? null : v;
    }

    private PresentacionResponseDTO toResponse(Presentacion p) {
        return PresentacionResponseDTO.builder()
                .id(p.getId())
                .productoId(p.getProducto().getId())
                .name(p.getName())
                .unitsBase(p.getUnitsBase())
                .type(p.getType())
                .price(p.getPrice())
                .stockMin(p.getStockMin())
                .active(p.getActive())
                .sortOrder(p.getSortOrder())
                .build();
    }

    ProductoResponseDTO toResponse(Producto p) {
        List<PresentacionResponseDTO> presentaciones = p.getPresentaciones().stream()
                .sorted(Comparator.comparing(Presentacion::getSortOrder)
                        .thenComparing(Presentacion::getId))
                .map(this::toResponse)
                .toList();

        // Margen teorico: se usa el precio de la presentacion VENTA mas barata,
        // que es la que realmente se vende en mostrador
        BigDecimal precioVenta = presentaciones.stream()
                .filter(x -> x.getType() == TipoPresentacion.VENTA && x.getPrice() != null)
                .map(PresentacionResponseDTO::getPrice)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        BigDecimal margen = BigDecimal.ZERO;
        if (precioVenta.compareTo(BigDecimal.ZERO) > 0) {
            margen = precioVenta.subtract(p.getCostAvg())
                    .divide(precioVenta, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Proximo vencimiento entre los lotes con saldo
        LocalDate primerVencimiento = loteDAO
                .findByProductoIdAndQtyRemainingGreaterThanOrderByExpiryDateAsc(p.getId(), 0)
                .stream()
                .map(Lote::getExpiryDate)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);

        Integer diasParaVencer = primerVencimiento == null
            ? null
            : (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), primerVencimiento);

        return ProductoResponseDTO.builder()
                .id(p.getId())
                .sku(p.getSku())
                .barcode(p.getBarcode())
                .name(p.getName())
                .description(p.getDescription())
                .categoriaId(p.getCategoria().getId())
                .categoriaNombre(p.getCategoria().getName())
                .marcaId(p.getMarca() == null ? null : p.getMarca().getId())
                .marcaNombre(p.getMarca() == null ? null : p.getMarca().getName())
                .baseUnit(p.getBaseUnit().name())
                .contentMl(p.getContentMl())
                .stock(p.getStock())
                .minStock(p.getMinStock())
                .maxStock(p.getMaxStock())
                .costAvg(p.getCostAvg())
                .active(p.getActive())
                .presentaciones(presentaciones)
                .margenTeoricoPct(margen)
                .primerVencimiento(primerVencimiento)
                .diasParaVencer(diasParaVencer)
                .build();
    }
}