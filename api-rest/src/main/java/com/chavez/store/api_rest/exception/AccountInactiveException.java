package com.chavez.store.api_rest.exception;

/** El usuario esta inactivo. HTTP 403 · AUTH_003 */
public class AccountInactiveException extends BusinessException {

    public AccountInactiveException() {
        super("AUTH_003", 403, "El usuario esta inactivo");
    }
}