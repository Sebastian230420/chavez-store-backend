package com.chavez.store.api_rest.exception;

/** Documento duplicado (cliente, proveedor). HTTP 409 · CLI_001 */
public class DuplicateDocumentException extends BusinessException {

    public DuplicateDocumentException(String documento) {
        super("CLI_001", 409, "El numero de documento ya esta registrado: " + documento);
    }
}