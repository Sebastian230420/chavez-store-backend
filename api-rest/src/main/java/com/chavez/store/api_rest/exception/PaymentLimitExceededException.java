package com.chavez.store.api_rest.exception;

import java.math.BigDecimal;

/** El credito supera el limite del cliente. HTTP 422 · SAL_003 */
public class PaymentLimitExceededException extends BusinessException {

    public PaymentLimitExceededException(String cliente, BigDecimal saldo, BigDecimal nuevoTotal, BigDecimal limite) {
        super("SAL_003", 422, "El credito supera el limite de " + cliente
                + ": saldo " + saldo + " + " + nuevoTotal + " > limite " + limite);
    }
}