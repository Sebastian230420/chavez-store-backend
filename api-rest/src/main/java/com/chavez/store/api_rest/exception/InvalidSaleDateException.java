package com.chavez.store.api_rest.exception;

/** Fecha de venta futura. HTTP 422 · SAL_006 */
public class InvalidSaleDateException extends BusinessException {

    public InvalidSaleDateException() {
        super("SAL_006", 422, "La fecha de venta no puede ser futura");
    }
}