package com.chavez.store.api_rest.repository.dao.impl;

import com.chavez.store.api_rest.repository.dao.IProductoDAOExtra;
import com.chavez.store.api_rest.entity.Producto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Consultas de inventario que no se resuelven por convencion de Spring Data.
 * Solo logica de persistencia: ninguna regla de negocio (ver LOGICA_NEGOCIO.md).
 */
@Repository
public class ProductoDAOImpl implements IProductoDAOExtra {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Regla R-I-04: products.stock (cacheado) debe coincidir con la suma del kardex.
     * Si no coincide, es una alerta de auditoria: NUNCA se corrige en silencio.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> verificarInvarianteStock() {
        String sql = """
                SELECT p.id, p.sku, p.name, p.stock, COALESCE(SUM(m.qty), 0) AS stock_real
                FROM products p
                LEFT JOIN stock_movements m ON m.product_id = p.id
                GROUP BY p.id, p.sku, p.name, p.stock
                HAVING p.stock <> COALESCE(SUM(m.qty), 0)
                ORDER BY p.sku
                """;
        return entityManager.createNativeQuery(sql).getResultList();
    }

    @Override
    public long stockRealDe(Long productoId) {
        String sql = "SELECT COALESCE(SUM(qty), 0) FROM stock_movements WHERE product_id = ?1";
        Object resultado = entityManager.createNativeQuery(sql)
                .setParameter(1, productoId)
                .getSingleResult();
        return ((Number) resultado).longValue();
    }

    /** Costo promedio del costo de las unidades vendidas: costo real, no estimado. */
    @Override
    public Double rotacionEnPeriodo(Long productoId, LocalDateTime desde, LocalDateTime hasta) {
        String sql = """
                SELECT CASE
                    WHEN SUM(CASE WHEN m.qty > 0 THEN m.qty ELSE 0 END) = 0 THEN 0
                    ELSE SUM(CASE WHEN m.qty < 0 THEN -m.qty ELSE 0 END) * 1.0
                         / SUM(CASE WHEN m.qty > 0 THEN m.qty ELSE 0 END)
                END
                FROM stock_movements m
                WHERE m.product_id = ?1 AND m.created_at BETWEEN ?2 AND ?3
                """;
        Object resultado = entityManager.createNativeQuery(sql)
                .setParameter(1, productoId)
                .setParameter(2, desde)
                .setParameter(3, hasta)
                .getSingleResult();
        return resultado == null ? 0.0 : ((Number) resultado).doubleValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Producto> findAllByIdIn(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return entityManager
                .createQuery("select p from Producto p where p.id in :ids", Producto.class)
                .setParameter("ids", ids)
                .getResultList();
    }
}