package com.chavez.store.api_rest.controller;

import com.chavez.store.api_rest.dto.response.CuentasPorCobrarResponseDTO;
import com.chavez.store.api_rest.dto.response.GananciasCategoriaResponseDTO;
import com.chavez.store.api_rest.dto.response.GananciasProductoResponseDTO;
import com.chavez.store.api_rest.dto.response.GananciasResponseDTO;
import com.chavez.store.api_rest.dto.response.MermasResponseDTO;
import com.chavez.store.api_rest.dto.response.StockReporteResponseDTO;
import com.chavez.store.api_rest.dto.response.VentasDelDiaResponseDTO;
import com.chavez.store.api_rest.dto.response.VentasPorHoraResponseDTO;
import com.chavez.store.api_rest.service.IReporteService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reportes de ganancias, mermas, stock y cobranza. */
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final IReporteService reporteService;

    /** R-R-02: utilidad = ingresos - costoVentas - mermas. */
    @GetMapping("/ganancias")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<GananciasResponseDTO> ganancias(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate d = desde == null ? LocalDate.now().withDayOfMonth(1) : desde;
        LocalDate h = hasta == null ? LocalDate.now() : hasta;
        return ResponseEntity.ok(reporteService.ganancias(d, h));
    }

    /** R-R-04: margen teorico vs margen real por categoria. */
    @GetMapping("/ganancias/categoria")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<GananciasCategoriaResponseDTO> gananciasPorCategoria(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate d = desde == null ? LocalDate.now().withDayOfMonth(1) : desde;
        LocalDate h = hasta == null ? LocalDate.now() : hasta;
        return ResponseEntity.ok(reporteService.gananciasPorCategoria(d, h));
    }

    @GetMapping("/ganancias/producto")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<GananciasProductoResponseDTO> gananciasPorProducto(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long categoriaId) {
        LocalDate d = desde == null ? LocalDate.now().withDayOfMonth(1) : desde;
        LocalDate h = hasta == null ? LocalDate.now() : hasta;
        return ResponseEntity.ok(reporteService.gananciasPorProducto(d, h, categoriaId));
    }

    /** R-R-06: dinero perdido por merma y su causa. */
    @GetMapping("/mermas")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<MermasResponseDTO> mermas(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String motivo) {
        LocalDate d = desde == null ? LocalDate.now().withDayOfMonth(1) : desde;
        LocalDate h = hasta == null ? LocalDate.now() : hasta;
        return ResponseEntity.ok(reporteService.mermas(d, h, motivo));
    }

    /** R-I-09 y R-R-07: stock critico y lotes por vencer. */
    @GetMapping("/stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'ALMACENERO')")
    public ResponseEntity<StockReporteResponseDTO> stock(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate hoy = LocalDate.now();
        LocalDate d = desde == null ? hoy : desde;
        LocalDate h = hasta == null ? hoy.plusDays(30) : hasta;
        return ResponseEntity.ok(reporteService.stock(d, h));
    }

    /** R-R-08: cuentas por cobrar con antiguedad. */
    @GetMapping("/cuentas-por-cobrar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CuentasPorCobrarResponseDTO> cuentasPorCobrar() {
        return ResponseEntity.ok(reporteService.cuentasPorCobrar());
    }

    /** R-R-09: que franja horaria mueve mas volumen. */
    @GetMapping("/ventas-por-hora")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<VentasPorHoraResponseDTO> ventasPorHora(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate d = desde == null ? LocalDate.now().withDayOfMonth(1) : desde;
        LocalDate h = hasta == null ? LocalDate.now() : hasta;
        return ResponseEntity.ok(reporteService.ventasPorHora(d, h));
    }

    /** Total vendido por dia. Sin arqueo ni caja. */
    @GetMapping("/ventas-del-dia")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<VentasDelDiaResponseDTO> ventasDelDia(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        LocalDate f = fecha == null ? LocalDate.now() : fecha;
        return ResponseEntity.ok(reporteService.ventasDelDia(f));
    }
}