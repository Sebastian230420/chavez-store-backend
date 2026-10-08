package com.chavez.store.api_rest.repository.dao;

import com.chavez.store.api_rest.entity.Producto;
import java.util.List;

/**
 * Methods que JPA no resuelve por convencion y requieren consultas compuestas.
 * Los implementa ProductoDAOImpl con EntityManager.
 */
public interface IProductoDAOExtra {

    /**
     * Verifica el invariante de inventario (regla R-I-04):
     * productos.stock debe igualar la suma de sus movimientos.
     *
     * @return lista de descuadres; vacia si todo cuadra
     */
    List<Object[]> verificarInvarianteStock();

    /** Suma de movimientos (con signo) de un producto. */
    long stockRealDe(Long productoId);

    /** Rotacion de inventario: veces que el stock se renovo en el periodo. */
    Double rotacionEnPeriodo(Long productoId, java.time.LocalDateTime desde,
                             java.time.LocalDateTime hasta);

    List<Producto> findAllByIdIn(List<Long> ids);
}