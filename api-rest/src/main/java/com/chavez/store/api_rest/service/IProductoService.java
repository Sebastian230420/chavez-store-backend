package com.chavez.store.api_rest.service;

import com.chavez.store.api_rest.dto.request.PresentacionRequestDTO;
import com.chavez.store.api_rest.dto.request.PresentacionUpdateRequestDTO;
import com.chavez.store.api_rest.dto.request.ProductoRequestDTO;
import com.chavez.store.api_rest.dto.request.ProductoUpdateRequestDTO;
import com.chavez.store.api_rest.dto.response.PageResponseDTO;
import com.chavez.store.api_rest.dto.response.PresentacionResponseDTO;
import com.chavez.store.api_rest.dto.response.ProductoResponseDTO;
import com.chavez.store.api_rest.entity.enums.TipoMovimientoStock;
import java.util.List;

public interface IProductoService {

    ProductoResponseDTO crear(ProductoRequestDTO request);

    ProductoResponseDTO actualizar(Long id, ProductoUpdateRequestDTO request);

    /** Regla R-C-07: no se elimina si tiene movimientos, se desactiva. */
    void desactivar(Long id);

    ProductoResponseDTO obtener(Long id);

    ProductoResponseDTO obtenerPorSku(String sku);

    List<ProductoResponseDTO> listarTodos();

    PageResponseDTO<ProductoResponseDTO> buscar(String search, Long categoriaId, Long marcaId,
                                               Boolean stockBajo, int page, int size);

    // ── Presentaciones ──
    List<PresentacionResponseDTO> listarPresentaciones(Long productoId);

    PresentacionResponseDTO agregarPresentacion(Long productoId, PresentacionRequestDTO request);

    PresentacionResponseDTO actualizarPresentacion(Long presentacionId, PresentacionUpdateRequestDTO request);

    void desactivarPresentacion(Long presentacionId);

    // ── Inventario ──
    ProductoResponseDTO registrarMovimiento(Long productoId, Integer cantidad,
                                            TipoMovimientoStock tipo, String motivo);

    ProductoResponseDTO verificarInvariante(Long productoId);
}