package com.chavez.store.api_rest.exception;

/** El token JWT expiro. HTTP 401 · AUTH_004 */
public class TokenExpiredException extends BusinessException {

    public TokenExpiredException() {
        super("AUTH_004", 401, "El token ha expirado");
    }
}