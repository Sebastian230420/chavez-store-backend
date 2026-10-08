package com.chavez.store.api_rest.service;

import com.chavez.store.api_rest.dto.request.AbonoRequestDTO;
import com.chavez.store.api_rest.dto.request.ClienteRequestDTO;
import com.chavez.store.api_rest.dto.request.LimiteCreditoRequestDTO;
import com.chavez.store.api_rest.dto.response.AbonoResponseDTO;
import com.chavez.store.api_rest.dto.response.ClienteDetalleResponseDTO;
import com.chavez.store.api_rest.dto.response.ClienteResponseDTO;
import com.chavez.store.api_rest.dto.response.PageResponseDTO;
import com.chavez.store.api_rest.entity.enums.TipoCliente;
import java.math.BigDecimal;
import java.util.List;

public interface IClienteService {

    ClienteResponseDTO crear(ClienteRequestDTO request);

    ClienteResponseDTO actualizar(Long id, ClienteRequestDTO request);

    /** Regla R-CL-08: con saldo pendiente solo se desactiva. */
    void desactivar(Long id);

    ClienteResponseDTO obtener(Long id);

    ClienteDetalleResponseDTO obtenerDetalle(Long id);

    PageResponseDTO<ClienteResponseDTO> buscar(String search, TipoCliente tipo, int page, int size);

    List<ClienteResponseDTO> listarActivos();

    /** Regla R-CL-04: solo ADMIN. */
    ClienteResponseDTO modificarLimiteCredito(Long id, LimiteCreditoRequestDTO request);

    BigDecimal saldoDe(Long clienteId);

    // ── Abonos: unica fuente de cobranza (R-CL-07) ──
    AbonoResponseDTO registrarAbono(Long clienteId, AbonoRequestDTO request);

    List<AbonoResponseDTO> listarAbonos(Long clienteId);

    /** Regla R-CL-09: solo ADMIN. */
    AbonoResponseDTO anularAbono(Long clienteId, Long abonoId);
}