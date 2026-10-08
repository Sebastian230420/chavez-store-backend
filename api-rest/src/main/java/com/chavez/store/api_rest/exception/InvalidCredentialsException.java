package com.chavez.store.api_rest.exception;

/** Credenciales invalidas. HTTP 401 · AUTH_001 */
public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super("AUTH_001", 401, "Credenciales invalidas");
    }
}