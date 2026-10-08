package com.chavez.store.api_rest.exception;

/** Cliente con saldo pendiente: no se elimina, se desactiva. HTTP 422 · CLI_002 */
public class CustomerHasDebtException extends BusinessException {

    public CustomerHasDebtException(String nombre, java.math.BigDecimal saldo) {
        super("CLI_002", 422, "El cliente " + nombre + " tiene saldo pendiente: "
                + saldo + ". No se elimina, se desactiva");
    }
}