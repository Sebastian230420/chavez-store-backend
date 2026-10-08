package com.chavez.store.api_rest.exception;

/** Recurso no encontrado. HTTP 404 · DAT_001 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super("DAT_001", 404, message);
    }

    public static ResourceNotFoundException of(String entidad, Object id) {
        return new ResourceNotFoundException(entidad + " no encontrado: " + id);
    }

    public static ResourceNotFoundException porNombre(String entidad, String nombre) {
        return new ResourceNotFoundException(entidad + " no encontrado: " + nombre);
    }
}