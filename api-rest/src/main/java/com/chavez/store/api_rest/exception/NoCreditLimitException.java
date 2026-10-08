package com.chavez.store.api_rest.exception;

/** El cliente no tiene cupo de credito. HTTP 422 · SAL_004 */
public class NoCreditLimitException extends BusinessException {

    public NoCreditLimitException(String cliente) {
        super("SAL_004", 422, "El cliente " + cliente + " no tiene cupo de credito");
    }
}