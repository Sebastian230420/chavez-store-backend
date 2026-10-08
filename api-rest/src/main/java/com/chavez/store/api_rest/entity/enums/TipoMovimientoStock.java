package com.chavez.store.api_rest.entity.enums;

import java.math.BigDecimal;

/**
 * Tipos de movimiento del kardex. Append-only: nunca se actualizan ni se borran.
 * Una anulacion de venta inserta un movimiento con el signo invertido.
 */
public enum TipoMovimientoStock {

    INVENTARIO_INICIAL(BigDecimal.ONE),
    COMPRA(BigDecimal.ONE),
    VENTA(BigDecimal.ONE.negate()),
    AJUSTE_POSITIVO(BigDecimal.ONE),
    AJUSTE_NEGATIVO(BigDecimal.ONE.negate()),
    MERMA(BigDecimal.ONE.negate()),
    DEVOLUCION_PROVEEDOR(BigDecimal.ONE);

    private final BigDecimal signo;

    TipoMovimientoStock(BigDecimal signo) {
        this.signo = signo;
    }

    public BigDecimal getSigno() {
        return signo;
    }

    /** Motivo obligatorio para ajustes y mermas. */
    public boolean exigeMotivo() {
        return this == MERMA || this == AJUSTE_POSITIVO || this == AJUSTE_NEGATIVO;
    }
}