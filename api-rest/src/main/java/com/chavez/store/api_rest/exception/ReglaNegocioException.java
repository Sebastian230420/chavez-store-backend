package com.chavez.store.api_rest.exception;

import java.math.BigDecimal;

/**
 * Violacion de una regla de negocio no cubierta por una excepcion especifica.
 * HTTP 400 · VAL_001
 */
public class ReglaNegocioException extends BusinessException {

    public ReglaNegocioException(String message) {
        super("VAL_001", 400, message);
    }

    public ReglaNegocioException(String message, Object... args) {
        super("VAL_001", 400, String.format(message, args));
    }

    public static ReglaNegocioException sinPresentacionVenta(Long productoId) {
        return new ReglaNegocioException(
                "El producto debe tener al menos una presentacion de tipo VENTA (R-C-04). Producto: " + productoId);
    }

    public static ReglaNegocioException presentacionVentaSinPrecio(String nombre) {
        return new ReglaNegocioException(
                "La presentacion de VENTA debe tener precio mayor a 0 (R-C-06): " + nombre);
    }

    public static ReglaNegocioException unitsBaseInvalido(String nombre) {
        return new ReglaNegocioException(
                "El factor de conversion unitsBase debe ser mayor a 0 (R-C-05): " + nombre);
    }

    public static ReglaNegocioException motivoObligatorio(String tipo) {
        return new ReglaNegocioException("Motivo obligatorio para el movimiento " + tipo + " (R-I-06/R-I-07)");
    }

    public static ReglaNegocioException montoNoPositivo(BigDecimal monto) {
        return new ReglaNegocioException("El monto debe ser mayor a 0: " + monto);
    }

    public static ReglaNegocioException clienteInactivo(String nombre) {
        return new ReglaNegocioException("El cliente esta inactivo: " + nombre);
    }

    public static ReglaNegocioException proveedorInactivo(String nombre) {
        return new ReglaNegocioException("El proveedor esta inactivo: " + nombre);
    }

    public static ReglaNegocioException stockBajoMinimo(String nombre, int stock, int minimo) {
        return new ReglaNegocioException("Stock de " + nombre + " por debajo del minimo: "
                + stock + " < " + minimo + " (R-I-09)");
    }

    public static ReglaNegocioException autoDesactivacionProhibida() {
        return new ReglaNegocioException("Un usuario no puede desactivar su propia cuenta (R-A-07)");
    }

    public static ReglaNegocioException compraVacia() {
        return new ReglaNegocioException("La compra debe tener al menos un item (R-CO-01)");
    }
}