package com.chavez.store.api_rest.exception;

/** La operacion dejaria el stock en negativo o no hay lotes suficientes. HTTP 422 · STK_002 */
public class NegativeStockException extends BusinessException {

    public NegativeStockException(String producto) {
        super("STK_002", 422, "No hay lotes suficientes para " + producto);
    }

    public NegativeStockException(String producto, int disponible, int solicitado) {
        super("STK_002", 422, "Stock insuficiente de " + producto
                + ": hay " + disponible + " unidades en lotes, se requieren " + solicitado);
    }
}