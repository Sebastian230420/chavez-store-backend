package com.chavez.store.api_rest.exception;

/** El usuario no tiene el rol requerido. HTTP 403 · AUTH_006 */
public class UnauthorizedRoleException extends BusinessException {

    public UnauthorizedRoleException(String operacion) {
        super("AUTH_006", 403, "No tiene permisos para " + operacion);
    }

    public UnauthorizedRoleException(String operacion, String rolesRequeridos) {
        super("AUTH_006", 403, "No tiene permisos para " + operacion
                + ". Requiere rol: " + rolesRequeridos);
    }
}