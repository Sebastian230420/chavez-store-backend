package com.chavez.store.api_rest.exception;

/** Transicion de compra no permitida. HTTP 422 · PUR_002 */
public class PurchaseStateException extends BusinessException {

    public PurchaseStateException(String mensaje) {
        super("PUR_002", 422, mensaje);
    }

    public static PurchaseStateException noRegistrada() {
        return new PurchaseStateException("La compra no esta en estado REGISTRADA");
    }

    public static PurchaseStateException yaRecibida() {
        return new PurchaseStateException("La compra ya fue recibida");
    }
}