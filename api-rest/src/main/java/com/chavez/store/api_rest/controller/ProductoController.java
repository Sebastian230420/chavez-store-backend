package com.chavez.store.api_rest.controller;

import com.chavez.store.api_rest.dto.request.PresentacionRequestDTO;
import com.chavez.store.api_rest.dto.request.PresentacionUpdateRequestDTO;
import com.chavez.store.api_rest.dto.request.ProductoRequestDTO;
import com.chavez.store.api_rest.dto.request.ProductoUpdateRequestDTO;
import com.chavez.store.api_rest.dto.response.MensajeResponseDTO;
import com.chavez.store.api_rest.dto.response.PageResponseDTO;
import com.chavez.store.api_rest.dto.response.PresentacionResponseDTO;
import com.chavez.store.api_rest.dto.response.ProductoResponseDTO;
import com.chavez.store.api_rest.service.IProductoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final IProductoService productoService;

    @GetMapping
    public ResponseEntity<PageResponseDTO<ProductoResponseDTO>> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long marcaId,
            @RequestParam(required = false) Boolean stockBajo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                productoService.buscar(search, categoriaId, marcaId, stockBajo, page, size));
    }

    @GetMapping("/todos")
    public ResponseEntity<List<ProductoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(productoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtener(id));
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductoResponseDTO> obtenerPorSku(@PathVariable String sku) {
        return ResponseEntity.ok(productoService.obtenerPorSku(sku));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ProductoResponseDTO> crear(@Valid @RequestBody ProductoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ProductoResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoUpdateRequestDTO request) {
        return ResponseEntity.ok(productoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MensajeResponseDTO> desactivar(@PathVariable Long id) {
        productoService.desactivar(id);
        return ResponseEntity.ok(MensajeResponseDTO.ok("Producto desactivado: " + id));
    }

    // ── Presentaciones ──

    @GetMapping("/{id}/presentaciones")
    public ResponseEntity<List<PresentacionResponseDTO>> listarPresentaciones(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.listarPresentaciones(id));
    }

    @PostMapping("/{id}/presentaciones")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<PresentacionResponseDTO> agregarPresentacion(
            @PathVariable Long id,
            @Valid @RequestBody PresentacionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productoService.agregarPresentacion(id, request));
    }

    @PutMapping("/presentaciones/{presentacionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<PresentacionResponseDTO> actualizarPresentacion(
            @PathVariable Long presentacionId,
            @Valid @RequestBody PresentacionUpdateRequestDTO request) {
        return ResponseEntity.ok(productoService.actualizarPresentacion(presentacionId, request));
    }

    @DeleteMapping("/presentaciones/{presentacionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MensajeResponseDTO> desactivarPresentacion(@PathVariable Long presentacionId) {
        productoService.desactivarPresentacion(presentacionId);
        return ResponseEntity.ok(MensajeResponseDTO.ok("Presentacion desactivada: " + presentacionId));
    }
}