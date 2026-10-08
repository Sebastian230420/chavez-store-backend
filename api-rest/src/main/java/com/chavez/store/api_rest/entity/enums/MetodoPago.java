package com.chavez.store.api_rest.entity.enums;

/**
 * Metodos de pago. Solo se usan en abonos: las ventas son un registro interno
 * y no registran forma de pago (regla R-V-14).
 */
public enum MetodoPago {
    EFECTIVO,
    TARJETA_DEBITO,
    TARJETA_CREDITO,
    YAPE,
    PLIN,
    TRANSFERENCIA
}