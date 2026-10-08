package com.chavez.store.api_rest.exception;

import lombok.Getter;

/**
 * Excepcion base de negocio. Todas las excepciones del catalogo extienden de esta.
 * El GlobalExceptionHandler la traduce a JSON con su codigo y estado HTTP.
 * Catalogo completo en LOGICA_NEGOCIO.md seccion 6.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final int httpStatus;

    public BusinessException(String code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}