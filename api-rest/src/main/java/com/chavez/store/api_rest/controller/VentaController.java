package com.chavez.store.api_rest.controller;

import com.chavez.store.api_rest.dto.request.AnularVentaRequestDTO;
import com.chavez.store.api_rest.dto.request.VentaRequestDTO;
import com.chavez.store.api_rest.dto.response.PageResponseDTO;
import com.chavez.store.api_rest.dto.response.VentaResponseDTO;
import com.chavez.store.api_rest.entity.enums.EstadoVenta;
import com.chavez.store.api_rest.entity.enums.TipoVenta;
import com.chavez.store.api_rest.service.IVentaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ventas: REGISTRO INTERNO.
 * No registra forma de pago ni genera voucher para el cliente final.
 */
@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final IVentaService ventaService;

    @GetMapping
    public ResponseEntity<PageResponseDTO<VentaResponseDTO>> listar(
            @RequestParam(required = false) EstadoVenta status,
            @RequestParam(required = false) TipoVenta type,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        LocalDateTime desdeDt = desde == null ? null : desde.atStartOfDay();
        LocalDateTime hastaDt = hasta == null ? null : hasta.atTime(LocalTime.MAX);

        return ResponseEntity.ok(ventaService.buscar(
                status, type, clienteId, desdeDt, hastaDt, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'SUPERVISOR')")
    public ResponseEntity<VentaResponseDTO> registrar(@Valid @RequestBody VentaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrar(request));
    }

    /** R-V-09: revierte el stock a los lotes originales. Solo ADMIN. */
    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VentaResponseDTO> anular(
            @PathVariable Long id,
            @Valid @RequestBody AnularVentaRequestDTO request) {
        return ResponseEntity.ok(ventaService.anular(id, request));
    }
}