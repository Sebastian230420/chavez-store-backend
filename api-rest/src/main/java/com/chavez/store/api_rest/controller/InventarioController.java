package com.chavez.store.api_rest.controller;

import com.chavez.store.api_rest.dto.request.AjusteStockRequestDTO;
import com.chavez.store.api_rest.dto.request.MermaRequestDTO;
import com.chavez.store.api_rest.dto.request.StockInicialRequestDTO;
import com.chavez.store.api_rest.dto.response.KardexResponseDTO;
import com.chavez.store.api_rest.dto.response.LoteStockResponseDTO;
import com.chavez.store.api_rest.dto.response.ProductoResponseDTO;
import com.chavez.store.api_rest.dto.response.StockProductoResponseDTO;
import com.chavez.store.api_rest.dto.response.VerificarStockResponseDTO;
import com.chavez.store.api_rest.service.IInventarioService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final IInventarioService inventarioService;

    /** Kardex de un producto con su stock resultante. */
    @GetMapping("/kardex/{productoId}")
    public ResponseEntity<Page<KardexResponseDTO>> kardex(
            @PathVariable Long productoId,
            @PageableDefault(size = 50, sort = "createdAt") org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(inventarioService.kardex(productoId, pageable));
    }

    /** Stock actual, lotes y estado (NORMAL / CRITICO / SIN_STOCK). */
    @GetMapping("/stock/{productoId}")
    public ResponseEntity<StockProductoResponseDTO> stock(@PathVariable Long productoId) {
        return ResponseEntity.ok(inventarioService.stockDe(productoId));
    }

    @GetMapping("/lotes/por-vencer")
    public ResponseEntity<List<LoteStockResponseDTO>> lotesPorVencer(
            @RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(inventarioService.lotesPorVencer(dias));
    }

    /** R-I-04: verifica que el stock cacheado cuadre con la suma del kardex. */
    @GetMapping("/verificar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VerificarStockResponseDTO> verificar() {
        return ResponseEntity.ok(inventarioService.verificarInvariante());
    }

    @GetMapping("/stock-critico")
    public ResponseEntity<List<ProductoResponseDTO>> stockCritico() {
        return ResponseEntity.ok(inventarioService.stockCritico());
    }

    /** R-I-07: registra una merma con motivo obligatorio. */
    @PostMapping("/mermas")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<KardexResponseDTO> registrarMerma(@Valid @RequestBody MermaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventarioService.registrarMerma(request));
    }

    /** R-I-06: ajuste manual con motivo obligatorio. */
    @PostMapping("/ajustes")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<KardexResponseDTO> registrarAjuste(@Valid @RequestBody AjusteStockRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventarioService.registrarAjuste(request));
    }

    /** Carga de inventario de apertura con sus lotes iniciales. */
    @PostMapping("/productos/{productoId}/stock-inicial")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ProductoResponseDTO> cargarStockInicial(
            @PathVariable Long productoId,
            @Valid @RequestBody StockInicialRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventarioService.cargarStockInicial(productoId, request));
    }
}