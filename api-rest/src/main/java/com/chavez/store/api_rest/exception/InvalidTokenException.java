package com.chavez.store.api_rest.exception;

/** Token invalido o malformado. HTTP 401 · AUTH_005 */
public class InvalidTokenException extends BusinessException {

    public InvalidTokenException() {
        super("AUTH_005", 401, "Token invalido o malformado");
    }
}