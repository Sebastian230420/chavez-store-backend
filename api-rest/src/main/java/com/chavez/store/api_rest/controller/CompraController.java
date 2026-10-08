package com.chavez.store.api_rest.controller;

import com.chavez.store.api_rest.dto.request.CompraRequestDTO;
import com.chavez.store.api_rest.dto.request.ProveedorRequestDTO;
import com.chavez.store.api_rest.dto.response.CompraResponseDTO;
import com.chavez.store.api_rest.dto.response.MensajeResponseDTO;
import com.chavez.store.api_rest.dto.response.PageResponseDTO;
import com.chavez.store.api_rest.dto.response.ProveedorResponseDTO;
import com.chavez.store.api_rest.entity.enums.EstadoCompra;
import com.chavez.store.api_rest.exception.DuplicateDocumentException;
import com.chavez.store.api_rest.exception.ReglaNegocioException;
import com.chavez.store.api_rest.exception.ResourceNotFoundException;
import com.chavez.store.api_rest.repository.dao.ICompraDAO;
import com.chavez.store.api_rest.repository.dao.IProveedorDAO;
import com.chavez.store.api_rest.service.ICompraService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CompraController {

    private final ICompraService compraService;
    private final IProveedorDAO proveedorDAO;
    private final ICompraDAO compraDAO;

    // ── Compras ──

    @GetMapping("/compras")
    public ResponseEntity<PageResponseDTO<CompraResponseDTO>> listar(
            @RequestParam(required = false) EstadoCompra status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(compraService.listar(status, page, size));
    }

    @GetMapping("/compras/{id}")
    public ResponseEntity<CompraResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.obtener(id));
    }

    @PostMapping("/compras")
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public ResponseEntity<CompraResponseDTO> registrar(@Valid @RequestBody CompraRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.registrar(request));
    }

    /** R-CO-03: crea lotes, suma stock y recalcula el costo promedio. */
    @PostMapping("/compras/{id}/recibir")
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public ResponseEntity<CompraResponseDTO> recibir(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.recibir(id));
    }

    @PatchMapping("/compras/{id}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompraResponseDTO> anular(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.anular(id));
    }

    @GetMapping("/proveedores/{proveedorId}/compras")
    public ResponseEntity<List<CompraResponseDTO>> comprasDeProveedor(
            @PathVariable Long proveedorId,
            @RequestParam(required = false) EstadoCompra status) {
        return ResponseEntity.ok(compraService.listarPorProveedor(proveedorId, status));
    }

    // ── Proveedores ──

    @GetMapping("/proveedores")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ProveedorResponseDTO>> listarProveedores(
            @RequestParam(required = false) String search) {
        List<com.chavez.store.api_rest.entity.Proveedor> proveedores;
        if (search == null || search.isBlank()) {
            proveedores = proveedorDAO.findByActiveTrueOrderByNameAsc();
        } else {
            proveedores = proveedorDAO.findByNameContainingIgnoreCaseOrderByNameAsc(search.trim());
        }

        List<ProveedorResponseDTO> lista = proveedores.stream()
                .map(p -> {
                    List<CompraResponseDTO> compras = compraService.listarPorProveedor(
                            p.getId(), EstadoCompra.RECIBIDA);
                    BigDecimal total = compras.stream()
                            .map(CompraResponseDTO::getTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return ProveedorResponseDTO.builder()
                            .id(p.getId())
                            .document(p.getDocument())
                            .name(p.getName())
                            .phone(p.getPhone())
                            .email(p.getEmail())
                            .address(p.getAddress())
                            .active(p.getActive())
                            .totalCompras((long) compras.size())
                            .totalComprado(total)
                            .ultimaCompra(compras.isEmpty() ? null : compras.get(0).getIssueDate())
                            .build();
                })
                .toList();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/proveedores/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ProveedorResponseDTO> obtenerProveedor(@PathVariable Long id) {
        var p = proveedorDAO.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Proveedor", id));
        return ResponseEntity.ok(ProveedorResponseDTO.builder()
                .id(p.getId())
                .document(p.getDocument())
                .name(p.getName())
                .phone(p.getPhone())
                .email(p.getEmail())
                .address(p.getAddress())
                .active(p.getActive())
                .build());
    }

    @PostMapping("/proveedores")
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public ResponseEntity<ProveedorResponseDTO> crearProveedor(@Valid @RequestBody ProveedorRequestDTO request) {
        // R-CO-06: documento autogenerado si no viene
        String documento = request.getDocument();
        if (documento == null || documento.isBlank()) {
            documento = "PROV-%06d".formatted(proveedorDAO.count() + 1);
        } else if (proveedorDAO.existsByDocument(documento)) {
            throw new DuplicateDocumentException(documento);
        }

        var p = proveedorDAO.save(com.chavez.store.api_rest.entity.Proveedor.builder()
                .document(documento)
                .name(request.getName().trim())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .active(true)
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(ProveedorResponseDTO.builder()
                .id(p.getId())
                .document(p.getDocument())
                .name(p.getName())
                .phone(p.getPhone())
                .email(p.getEmail())
                .address(p.getAddress())
                .active(p.getActive())
                .build());
    }

    @PutMapping("/proveedores/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public ResponseEntity<ProveedorResponseDTO> actualizarProveedor(
            @PathVariable Long id,
            @Valid @RequestBody ProveedorRequestDTO request) {
        var p = proveedorDAO.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Proveedor", id));
        p.setName(request.getName().trim());
        p.setPhone(request.getPhone());
        p.setEmail(request.getEmail());
        p.setAddress(request.getAddress());
        proveedorDAO.save(p);

        return ResponseEntity.ok(ProveedorResponseDTO.builder()
                .id(p.getId())
                .document(p.getDocument())
                .name(p.getName())
                .phone(p.getPhone())
                .email(p.getEmail())
                .address(p.getAddress())
                .active(p.getActive())
                .build());
    }

    /** R-CO-09: proveedor inactivo no recibe compras nuevas. */
    @DeleteMapping("/proveedores/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MensajeResponseDTO> desactivarProveedor(@PathVariable Long id) {
        var p = proveedorDAO.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Proveedor", id));
        p.setActive(false);
        proveedorDAO.save(p);
        return ResponseEntity.ok(MensajeResponseDTO.ok("Proveedor desactivado: " + id));
    }
}