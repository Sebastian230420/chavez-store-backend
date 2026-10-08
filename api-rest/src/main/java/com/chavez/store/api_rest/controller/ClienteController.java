package com.chavez.store.api_rest.controller;

import com.chavez.store.api_rest.dto.request.AbonoRequestDTO;
import com.chavez.store.api_rest.dto.request.ClienteRequestDTO;
import com.chavez.store.api_rest.dto.request.LimiteCreditoRequestDTO;
import com.chavez.store.api_rest.dto.response.AbonoResponseDTO;
import com.chavez.store.api_rest.dto.response.ClienteDetalleResponseDTO;
import com.chavez.store.api_rest.dto.response.ClienteResponseDTO;
import com.chavez.store.api_rest.dto.response.MensajeResponseDTO;
import com.chavez.store.api_rest.dto.response.PageResponseDTO;
import com.chavez.store.api_rest.entity.enums.TipoCliente;
import com.chavez.store.api_rest.service.IClienteService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final IClienteService clienteService;

    @GetMapping
    public ResponseEntity<PageResponseDTO<ClienteResponseDTO>> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TipoCliente type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(clienteService.buscar(search, type, page, size));
    }

    @GetMapping("/activos")
    public ResponseEntity<List<ClienteResponseDTO>> listarActivos() {
        return ResponseEntity.ok(clienteService.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtener(id));
    }

    /** Detalle con historial de ventas y abonos. */
    @GetMapping("/{id}/detalle")
    public ResponseEntity<ClienteDetalleResponseDTO> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerDetalle(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'SUPERVISOR')")
    public ResponseEntity<ClienteResponseDTO> crear(@Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'SUPERVISOR')")
    public ResponseEntity<ClienteResponseDTO> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MensajeResponseDTO> desactivar(@PathVariable Long id) {
        clienteService.desactivar(id);
        return ResponseEntity.ok(MensajeResponseDTO.ok("Cliente desactivado: " + id));
    }

    /** R-CL-04: solo ADMIN modifica el cupo de credito. */
    @PutMapping("/{id}/limite-credito")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClienteResponseDTO> modificarLimite(
            @PathVariable Long id,
            @Valid @RequestBody LimiteCreditoRequestDTO request) {
        return ResponseEntity.ok(clienteService.modificarLimiteCredito(id, request));
    }

    // ── Abonos: unica fuente de cobranza ──

    @PostMapping("/{id}/abonos")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'SUPERVISOR')")
    public ResponseEntity<AbonoResponseDTO> registrarAbono(
            @PathVariable Long id,
            @Valid @RequestBody AbonoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clienteService.registrarAbono(id, request));
    }

    @GetMapping("/{id}/abonos")
    public ResponseEntity<List<AbonoResponseDTO>> listarAbonos(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.listarAbonos(id));
    }

    /** R-CL-09: solo ADMIN. */
    @PatchMapping("/{id}/abonos/{abonoId}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AbonoResponseDTO> anularAbono(@PathVariable Long id, @PathVariable Long abonoId) {
        return ResponseEntity.ok(clienteService.anularAbono(id, abonoId));
    }
}