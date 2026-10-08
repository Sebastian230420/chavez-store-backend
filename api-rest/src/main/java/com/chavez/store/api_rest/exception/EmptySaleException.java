package com.chavez.store.api_rest.exception;

/** Venta sin items. HTTP 422 · SAL_001 */
public class EmptySaleException extends BusinessException {

    public EmptySaleException() {
        super("SAL_001", 422, "La venta debe tener al menos un item");
    }
}