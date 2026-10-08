package com.chavez.store.api_rest.service;

import com.chavez.store.api_rest.dto.request.CompraRequestDTO;
import com.chavez.store.api_rest.dto.response.CompraResponseDTO;
import com.chavez.store.api_rest.dto.response.PageResponseDTO;
import com.chavez.store.api_rest.entity.enums.EstadoCompra;
import java.util.List;

public interface ICompraService {

    CompraResponseDTO registrar(CompraRequestDTO request);

    /** Regla R-CO-03: crea lotes, suma stock y recalcula el costo promedio. */
    CompraResponseDTO recibir(Long id);

    /** Regla R-CO-05: devuelve el stock y recalcula el costo promedio. */
    CompraResponseDTO anular(Long id);

    CompraResponseDTO obtener(Long id);

    PageResponseDTO<CompraResponseDTO> listar(EstadoCompra status, int page, int size);

    List<CompraResponseDTO> listarPorProveedor(Long proveedorId, EstadoCompra status);
}