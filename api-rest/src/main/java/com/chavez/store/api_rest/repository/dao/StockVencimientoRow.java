package com.chavez.store.api_rest.repository.dao;

import java.time.LocalDate;
import java.util.List;

/** Projection del reporte de stock: critico y por vencer. */
public interface StockVencimientoRow {

    Long getProductoId();

    String getSku();

    String getNombre();

    Integer getQtyRemaining();

    LocalDate getExpiryDate();

    Double getCostoUnit();
}