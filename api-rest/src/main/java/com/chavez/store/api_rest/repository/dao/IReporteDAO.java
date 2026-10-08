package com.chavez.store.api_rest.repository.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Reportes de ganancias. Requiere agregaciones que JPQL no expresa bien. */
public interface IReporteDAO {

    /** Columnas: categoriaId, categoriaNombre, ventasCount, unidades, litros, ingresos, costo, mermas. */
    List<Object[]> gananciasPorCategoria(LocalDateTime desde, LocalDateTime hasta);

    /** Columnas: productoId, sku, nombre, categoria, unidades, ingresos, costo, mermas. */
    List<Object[]> gananciasPorProducto(LocalDateTime desde, LocalDateTime hasta, Long categoriaId);

    /** Columnas: fecha, ventasCount, unidades, ingresos, costo. */
    List<Object[]> gananciasPorDia(LocalDateTime desde, LocalDateTime hasta);

    /** Columnas: fecha, unidades, costo. */
    List<Object[]> mermasPorDia(LocalDateTime desde, LocalDateTime hasta);

    List<Object[]> mermasPorMotivo(LocalDateTime desde, LocalDateTime hasta);

    List<Object[]> mermasPorProducto(LocalDateTime desde, LocalDateTime hasta);

    List<Object[]> ventasPorHora(LocalDateTime desde, LocalDateTime hasta);

    /** Columnas: clienteId, fechaVenta, total. Los dias se calculan en Java. */
    List<Object[]> antiguedadDeuda(LocalDateTime hoy);

    List<StockVencimientoRow> lotesPorVencer(LocalDate desde, LocalDate hasta);
}