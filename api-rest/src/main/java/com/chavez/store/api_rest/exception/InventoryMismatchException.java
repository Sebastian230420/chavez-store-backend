package com.chavez.store.api_rest.exception;

/** El stock cacheado no cuadra con el kardex. HTTP 500 · STK_003 */
public class InventoryMismatchException extends BusinessException {

    public InventoryMismatchException(String sku, int stockCacheado, int stockReal) {
        super("STK_003", 500, "El stock de " + sku + " no cuadra con los movimientos: "
                + "cacheado " + stockCacheado + ", real " + stockReal);
    }
}