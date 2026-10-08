package com.chavez.store.api_rest.exception;

/** Abono que supera la deuda pendiente. HTTP 422 · SAL_005 */
public class InvalidPaymentAmountException extends BusinessException {

    public InvalidPaymentAmountException(String cliente, java.math.BigDecimal abono, java.math.BigDecimal saldo) {
        super("SAL_005", 422, "El abono de " + abono + " supera la deuda pendiente de "
                + cliente + " (" + saldo + ")");
    }
}