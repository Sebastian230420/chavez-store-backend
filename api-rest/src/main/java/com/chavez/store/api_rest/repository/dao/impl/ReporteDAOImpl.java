package com.chavez.store.api_rest.repository.dao.impl;

import com.chavez.store.api_rest.repository.dao.IReporteDAO;
import com.chavez.store.api_rest.repository.dao.StockVencimientoRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * Consultas nativas de reportes.
 *
 * Reglas aplicadas en SQL:
 *  - Solo ventas PAGADA (R-R-01): las anuladas se excluyen.
 *  - El costo sale del snapshot en sale_details.unit_cost, nunca del costo actual (R-R-03).
 *  - Las mermas salen de stock_movements tipo MERMA (R-R-06).
 */
@Repository
public class ReporteDAOImpl implements IReporteDAO {

    @PersistenceContext
    private EntityManager entityManager;

    /** Unidades * content_ml / 1000 = litros. Permite comparar volumen entre categorias. */
    private static final String SQL_LITROS =
            "COALESCE(SUM(sd.qty * sd.units_base * COALESCE(p.content_ml, 0)) / 1000.0, 0)";

    /**
     * Ganancias por categoria.
     * Devuelve Object[] en el orden de las columnas; el servicio arma el DTO.
     * Columnas: categoriaId, categoriaNombre, ventasCount, unidadesVendidas,
     *            litrosVendidos, ingresos, costoVentas, mermas
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> gananciasPorCategoria(LocalDateTime desde, LocalDateTime hasta) {
        // Mermas por categoria en consulta aparte: evita una subconsulta correlacionada
        // sobre c.id y cumple ONLY_FULL_GROUP_BY de MySQL 9.
        Map<Long, Double> mermas = new HashMap<>();
        for (Object[] fila : mermasPorCategoria_(desde, hasta)) {
            mermas.put(((Number) fila[0]).longValue(), ((Number) fila[1]).doubleValue());
        }

        String sql = """
                SELECT c.id                AS categoriaId,
                       c.name              AS categoriaNombre,
                       COUNT(DISTINCT s.id) AS ventasCount,
                       COALESCE(SUM(sd.qty * sd.units_base), 0) AS unidadesVendidas,
                       """
                + SQL_LITROS + """
                       AS litrosVendidos,
                       COALESCE(SUM(sd.subtotal), 0) AS ingresos,
                       COALESCE(SUM(sd.qty * sd.units_base * sd.unit_cost), 0) AS costoVentas
                FROM sale_details sd
                JOIN sales s      ON s.id = sd.sale_id
                JOIN products p   ON p.id = sd.product_id
                JOIN categories c ON c.id = p.category_id
                WHERE s.status = 'PAGADA'
                  AND s.sale_date BETWEEN ?1 AND ?2
                GROUP BY c.id, c.name
                ORDER BY ingresos DESC
                """;

        List<Object[]> crudos = entityManager.createNativeQuery(sql)
                .setParameter(1, desde)
                .setParameter(2, hasta)
                .getResultList();

        List<Object[]> resultado = new ArrayList<>(crudos.size());
        for (Object[] f : crudos) {
            long categoriaId = ((Number) f[0]).longValue();
            Object[] fila = Arrays.copyOf(f, 8);
            fila[7] = mermas.getOrDefault(categoriaId, 0.0d);
            resultado.add(fila);
        }
        return resultado;
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> mermasPorCategoria_(LocalDateTime desde, LocalDateTime hasta) {
        String sql = """
                SELECT p.category_id AS categoriaId,
                       COALESCE(SUM(-m.qty * m.unit_cost), 0) AS costoPerdido
                FROM stock_movements m
                JOIN products p ON p.id = m.product_id
                WHERE m.type = 'MERMA'
                  AND m.created_at BETWEEN ?1 AND ?2
                GROUP BY p.category_id
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter(1, desde)
                .setParameter(2, hasta)
                .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> gananciasPorProducto(LocalDateTime desde, LocalDateTime hasta, Long categoriaId) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.id AS productoId,
                       p.sku AS sku,
                       p.name AS nombre,
                       c.name AS categoria,
                       COALESCE(SUM(sd.qty * sd.units_base), 0) AS unidades,
                       COALESCE(SUM(sd.subtotal), 0) AS ingresos,
                       COALESCE(SUM(sd.qty * sd.units_base * sd.unit_cost), 0) AS costo,
                       COALESCE((
                           SELECT SUM(m.qty * m.unit_cost)
                           FROM stock_movements m
                           WHERE m.product_id = p.id AND m.type = 'MERMA'
                             AND m.created_at BETWEEN ?1 AND ?2
                       ), 0) AS mermas
                FROM sale_details sd
                JOIN sales s      ON s.id = sd.sale_id
                JOIN products p   ON p.id = sd.product_id
                JOIN categories c ON c.id = p.category_id
                WHERE s.status = 'PAGADA'
                  AND s.sale_date BETWEEN ?1 AND ?2
                """);
        if (categoriaId != null) {
            sql.append(" AND p.category_id = ?3 ");
        }
        sql.append(" GROUP BY p.id, p.sku, p.name, c.name ORDER BY ingresos DESC ");

        var query = entityManager.createNativeQuery(sql.toString())
                .setParameter(1, desde)
                .setParameter(2, hasta);
        if (categoriaId != null) {
            query.setParameter(3, categoriaId);
        }
        return query.getResultList();
    }

    /**
     * Ventas agregadas por dia.
     * Las mermas se consultan aparte (ver mermasPorDia) porque MySQL 9 exige
     * ONLY_FULL_GROUP_BY: una subconsulta correlacionada sobre s.sale_date
     * no es valida cuando solo se agrupa por DATE(s.sale_date).
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> gananciasPorDia(LocalDateTime desde, LocalDateTime hasta) {
        String sql = """
                SELECT DATE(s.sale_date) AS fecha,
                       COUNT(DISTINCT s.id) AS ventasCount,
                       COALESCE(SUM(sd.qty * sd.units_base), 0) AS unidades,
                       COALESCE(SUM(sd.subtotal), 0) AS ingresos,
                       COALESCE(SUM(sd.qty * sd.units_base * sd.unit_cost), 0) AS costo
                FROM sale_details sd
                JOIN sales s ON s.id = sd.sale_id
                WHERE s.status = 'PAGADA'
                  AND s.sale_date BETWEEN ?1 AND ?2
                GROUP BY DATE(s.sale_date)
                ORDER BY fecha
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter(1, desde)
                .setParameter(2, hasta)
                .getResultList();
    }

    /** Mermas agrupadas por dia, para cruzarlas con las ventas. */
    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> mermasPorDia(LocalDateTime desde, LocalDateTime hasta) {
        String sql = """
                SELECT DATE(m.created_at) AS fecha,
                       COALESCE(SUM(-m.qty), 0) AS unidades,
                       COALESCE(SUM(-m.qty * m.unit_cost), 0) AS costo
                FROM stock_movements m
                WHERE m.type = 'MERMA'
                  AND m.created_at BETWEEN ?1 AND ?2
                GROUP BY DATE(m.created_at)
                ORDER BY fecha
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter(1, desde)
                .setParameter(2, hasta)
                .getResultList();
    }

    /** El motivo va en el campo reason del movimiento (catalogo: VENCIDO, QUIEBRE, ...). */
    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> mermasPorMotivo(LocalDateTime desde, LocalDateTime hasta) {
        String sql = """
                SELECT COALESCE(m.reason, 'SIN_MOTIVO') AS motivo,
                       COALESCE(SUM(-m.qty), 0) AS unidades,
                       COALESCE(SUM(-m.qty * m.unit_cost), 0) AS costoPerdido
                FROM stock_movements m
                WHERE m.type = 'MERMA'
                  AND m.created_at BETWEEN ?1 AND ?2
                GROUP BY COALESCE(m.reason, 'SIN_MOTIVO')
                ORDER BY costoPerdido DESC
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter(1, desde)
                .setParameter(2, hasta)
                .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> mermasPorProducto(LocalDateTime desde, LocalDateTime hasta) {
        String sql = """
                SELECT p.id AS productoId,
                       p.name AS nombre,
                       COALESCE(SUM(-m.qty), 0) AS unidades,
                       COALESCE(SUM(-m.qty * m.unit_cost), 0) AS costoPerdido
                FROM stock_movements m
                JOIN products p ON p.id = m.product_id
                WHERE m.type = 'MERMA'
                  AND m.created_at BETWEEN ?1 AND ?2
                GROUP BY p.id, p.name
                ORDER BY costoPerdido DESC
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter(1, desde)
                .setParameter(2, hasta)
                .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> ventasPorHora(LocalDateTime desde, LocalDateTime hasta) {
        String sql = """
                SELECT HOUR(s.sale_date) AS hora,
                       COUNT(DISTINCT s.id) AS ventasCount,
                       COALESCE(SUM(sd.subtotal), 0) AS ingresos,
                       COALESCE(SUM(sd.qty * sd.units_base * sd.unit_cost), 0) AS costo
                FROM sale_details sd
                JOIN sales s ON s.id = sd.sale_id
                WHERE s.status = 'PAGADA'
                  AND s.sale_date BETWEEN ?1 AND ?2
                GROUP BY HOUR(s.sale_date)
                ORDER BY hora
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter(1, desde)
                .setParameter(2, hasta)
                .getResultList();
    }

    /**
     * Ventas a credito vigentes, base del reporte de cobranza.
     * Los dias de antiguedad se calculan en Java: evita DATEDIFF con parametro
     * posicional, que Hibernate reescribe de forma inconsistente en queries nativas.
     * Columnas: clienteId, fechaVenta, total
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> antiguedadDeuda(LocalDateTime hoy) {
        String sql = """
                SELECT v.customer_id, v.sale_date, v.total
                FROM sales v
                WHERE v.type = 'CREDITO'
                  AND v.status = 'PAGADA'
                ORDER BY v.sale_date ASC
                """;
        return entityManager.createNativeQuery(sql).getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<StockVencimientoRow> lotesPorVencer(LocalDate desde, LocalDate hasta) {
        String sql = """
                SELECT p.id AS productoId,
                       p.sku AS sku,
                       p.name AS nombre,
                       l.qty_remaining AS qtyRemaining,
                       l.expiry_date AS expiryDate,
                       l.cost_unit AS costoUnit
                FROM lots l
                JOIN products p ON p.id = l.product_id
                WHERE l.qty_remaining > 0
                  AND l.expiry_date IS NOT NULL
                  AND l.expiry_date BETWEEN ?1 AND ?2
                ORDER BY l.expiry_date ASC
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter(1, desde)
                .setParameter(2, hasta)
                .unwrap(org.hibernate.query.NativeQuery.class)
                .addScalar("productoId", Long.class)
                .addScalar("sku", String.class)
                .addScalar("nombre", String.class)
                .addScalar("qtyRemaining", Integer.class)
                .addScalar("expiryDate", LocalDate.class)
                .addScalar("costoUnit", Double.class)
                .getResultList();
    }
}