package com.chavez.store.api_rest.exception;

/** Stock insuficiente. HTTP 422 · STK_001 */
public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(String producto, int disponible, int solicitado) {
        super("STK_001", 422, "Stock insuficiente de " + producto
                + ": disponible " + disponible + ", solicitado " + solicitado);
    }
}