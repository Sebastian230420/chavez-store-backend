package com.chavez.store.api_rest.service;

import com.chavez.store.api_rest.dto.response.CuentasPorCobrarResponseDTO;
import com.chavez.store.api_rest.dto.response.GananciasCategoriaResponseDTO;
import com.chavez.store.api_rest.dto.response.GananciasProductoResponseDTO;
import com.chavez.store.api_rest.dto.response.GananciasResponseDTO;
import com.chavez.store.api_rest.dto.response.MermasResponseDTO;
import com.chavez.store.api_rest.dto.response.StockReporteResponseDTO;
import com.chavez.store.api_rest.dto.response.VentasDelDiaResponseDTO;
import com.chavez.store.api_rest.dto.response.VentasPorHoraResponseDTO;
import java.time.LocalDate;

public interface IReporteService {

    /** R-R-02: utilidad = ingresos - costoVentas - mermas. */
    GananciasResponseDTO ganancias(LocalDate desde, LocalDate hasta);

    /** R-R-04 y R-R-05: margen teorico vs margen real por categoria. */
    GananciasCategoriaResponseDTO gananciasPorCategoria(LocalDate desde, LocalDate hasta);

    GananciasProductoResponseDTO gananciasPorProducto(LocalDate desde, LocalDate hasta, Long categoriaId);

    MermasResponseDTO mermas(LocalDate desde, LocalDate hasta, String motivo);

    StockReporteResponseDTO stock(LocalDate desde, LocalDate hasta);

    CuentasPorCobrarResponseDTO cuentasPorCobrar();

    VentasPorHoraResponseDTO ventasPorHora(LocalDate desde, LocalDate hasta);

    VentasDelDiaResponseDTO ventasDelDia(LocalDate fecha);
}