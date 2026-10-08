package com.chavez.store.api_rest.controller;

import com.chavez.store.api_rest.dto.request.NombreRequestDTO;
import com.chavez.store.api_rest.dto.response.CategoriaResponseDTO;
import com.chavez.store.api_rest.dto.response.MarcaResponseDTO;
import com.chavez.store.api_rest.dto.response.MensajeResponseDTO;
import com.chavez.store.api_rest.entity.Categoria;
import com.chavez.store.api_rest.entity.Marca;
import com.chavez.store.api_rest.exception.ReglaNegocioException;
import com.chavez.store.api_rest.exception.ResourceNotFoundException;
import com.chavez.store.api_rest.repository.dao.ICategoriaDAO;
import com.chavez.store.api_rest.repository.dao.IMarcaDAO;
import com.chavez.store.api_rest.repository.dao.IProductoDAO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Categorias y marcas: catalogos simples, logica trivial. */
@RestController
@RequiredArgsConstructor
public class CategoriaController {

    private final ICategoriaDAO categoriaDAO;
    private final IMarcaDAO marcaDAO;
    private final IProductoDAO productoDAO;

    @GetMapping("/api/categorias")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CategoriaResponseDTO>> listar() {
        List<CategoriaResponseDTO> lista = categoriaDAO.findByActiveTrueOrderByNameAsc().stream()
                .map(c -> CategoriaResponseDTO.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .description(c.getDescription())
                        .active(c.getActive())
                        .totalProductos(productoDAO.findByActiveTrueOrderByNameAsc().stream()
                                .filter(p -> p.getCategoria().getId().equals(c.getId()))
                                .count())
                        .build())
                .toList();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/api/categorias/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<CategoriaResponseDTO> obtener(@PathVariable Long id) {
        Categoria c = categoriaDAO.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Categoria", id));
        return ResponseEntity.ok(CategoriaResponseDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .active(c.getActive())
                .build());
    }

    @PostMapping("/api/categorias")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<CategoriaResponseDTO> crear(@Valid @RequestBody NombreRequestDTO request) {
        if (categoriaDAO.existsByNameIgnoreCase(request.getName())) {
            throw new ReglaNegocioException("La categoria ya existe: " + request.getName());
        }
        Categoria c = categoriaDAO.save(Categoria.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .active(true)
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoriaResponseDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .active(c.getActive())
                .build());
    }

    @PutMapping("/api/categorias/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<CategoriaResponseDTO> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody NombreRequestDTO request) {
        Categoria c = categoriaDAO.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Categoria", id));
        c.setName(request.getName().trim());
        c.setDescription(request.getDescription());
        categoriaDAO.save(c);
        return ResponseEntity.ok(CategoriaResponseDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .active(c.getActive())
                .build());
    }

    @DeleteMapping("/api/categorias/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MensajeResponseDTO> desactivar(@PathVariable Long id) {
        Categoria c = categoriaDAO.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Categoria", id));
        c.setActive(false);
        categoriaDAO.save(c);
        return ResponseEntity.ok(MensajeResponseDTO.ok("Categoria desactivada: " + id));
    }

    // ── Marcas ──

    @GetMapping("/api/marcas")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MarcaResponseDTO>> listarMarcas() {
        return ResponseEntity.ok(marcaDAO.findByActiveTrueOrderByNameAsc().stream()
                .map(m -> MarcaResponseDTO.builder()
                        .id(m.getId())
                        .name(m.getName())
                        .active(m.getActive())
                        .build())
                .toList());
    }

    @PostMapping("/api/marcas")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<MarcaResponseDTO> crearMarca(@Valid @RequestBody NombreRequestDTO request) {
        if (marcaDAO.existsByNameIgnoreCase(request.getName())) {
            throw new ReglaNegocioException("La marca ya existe: " + request.getName());
        }
        Marca m = marcaDAO.save(Marca.builder().name(request.getName().trim()).active(true).build());
        return ResponseEntity.status(HttpStatus.CREATED).body(MarcaResponseDTO.builder()
                .id(m.getId())
                .name(m.getName())
                .active(m.getActive())
                .build());
    }

    @DeleteMapping("/api/marcas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MensajeResponseDTO> desactivarMarca(@PathVariable Long id) {
        Marca m = marcaDAO.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Marca", id));
        m.setActive(false);
        marcaDAO.save(m);
        return ResponseEntity.ok(MensajeResponseDTO.ok("Marca desactivada: " + id));
    }
}