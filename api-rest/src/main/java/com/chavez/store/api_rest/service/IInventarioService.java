package com.chavez.store.api_rest.service;

import com.chavez.store.api_rest.dto.request.AjusteStockRequestDTO;
import com.chavez.store.api_rest.dto.request.MermaRequestDTO;
import com.chavez.store.api_rest.dto.request.StockInicialRequestDTO;
import com.chavez.store.api_rest.dto.response.KardexResponseDTO;
import com.chavez.store.api_rest.dto.response.LoteStockResponseDTO;
import com.chavez.store.api_rest.dto.response.MermaResponseDTO;
import com.chavez.store.api_rest.dto.response.ProductoResponseDTO;
import com.chavez.store.api_rest.dto.response.StockProductoResponseDTO;
import com.chavez.store.api_rest.dto.response.VerificarStockResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IInventarioService {

    /** Regla R-I-10: descuenta del lote que vence primero. */
    KardexResponseDTO registrarMerma(MermaRequestDTO request);

    /** Regla R-I-06: exige motivo y rol ADMIN o SUPERVISOR. */
    KardexResponseDTO registrarAjuste(AjusteStockRequestDTO request);

    ProductoResponseDTO cargarStockInicial(Long productoId, StockInicialRequestDTO request);

    Page<KardexResponseDTO> kardex(Long productoId, Pageable pageable);

    StockProductoResponseDTO stockDe(Long productoId);

    java.util.List<LoteStockResponseDTO> lotesPorVencer(int dias);

    /** Regla R-I-04: verifica que el stock cacheado cuadre con el kardex. */
    VerificarStockResponseDTO verificarInvariante();

    java.util.List<ProductoResponseDTO> stockCritico();
}