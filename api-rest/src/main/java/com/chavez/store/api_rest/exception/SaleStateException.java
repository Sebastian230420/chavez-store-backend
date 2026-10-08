package com.chavez.store.api_rest.exception;

/** Transicion de estado no permitida (ventas y compras). HTTP 422 · SAL_002 / PUR_002 */
public class SaleStateException extends BusinessException {

    public SaleStateException(String mensaje) {
        super("SAL_002", 422, mensaje);
    }

    public SaleStateException(String mensaje, Object[] args) {
        super("SAL_002", 422, String.format(mensaje, args));
    }

    public static SaleStateException ventaYaAnulada() {
        return new SaleStateException("La venta ya esta ANULADA");
    }

    public static SaleStateException productoInactivo(String producto) {
        return new SaleStateException("Producto inactivo o presentacion sin precio: " + producto);
    }
}