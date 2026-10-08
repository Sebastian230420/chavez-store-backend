package com.chavez.store.api_rest.exception;

/** Producto con movimientos asociados: no se elimina, se desactiva. HTTP 409 · PROD_003 */
public class ProductInUseException extends BusinessException {

    public ProductInUseException(String nombre) {
        super("PROD_003", 409, "El producto " + nombre
                + " tiene movimientos o ventas asociados: no se elimina, se desactiva");
    }
}