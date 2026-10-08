package com.chavez.store.api_rest.service;

import com.chavez.store.api_rest.dto.request.AnularVentaRequestDTO;
import com.chavez.store.api_rest.dto.request.VentaRequestDTO;
import com.chavez.store.api_rest.dto.response.PageResponseDTO;
import com.chavez.store.api_rest.dto.response.VentaResponseDTO;
import com.chavez.store.api_rest.entity.enums.EstadoVenta;
import com.chavez.store.api_rest.entity.enums.TipoVenta;
import java.time.LocalDateTime;
import java.util.List;

public interface IVentaService {

    /**
     * Registro interno de venta (reglas R-V-01 a R-V-18).
     * No registra forma de pago ni genera voucher.
     */
    VentaResponseDTO registrar(VentaRequestDTO request);

    /** Regla R-V-09: revierte el stock a los lotes originales. */
    VentaResponseDTO anular(Long id, AnularVentaRequestDTO request);

    VentaResponseDTO obtener(Long id);

    PageResponseDTO<VentaResponseDTO> buscar(EstadoVenta status, TipoVenta tipo, Long clienteId,
                                             LocalDateTime desde, LocalDateTime hasta,
                                             int page, int size);

    List<VentaResponseDTO> listarPorCliente(Long clienteId);
}