package com.chavez.store.api_rest.repository.dao;

import com.chavez.store.api_rest.entity.enums.TipoMovimientoStock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IMovimientoDAO extends JpaRepository<com.chavez.store.api_rest.entity.MovimientoStock, Long> {

    Page<com.chavez.store.api_rest.entity.MovimientoStock> findByProductoIdOrderByCreatedAtDescIdDesc(
            Long productoId, Pageable pageable);

    List<com.chavez.store.api_rest.entity.MovimientoStock> findByRefTableAndRefId(String refTable, Long refId);

    List<com.chavez.store.api_rest.entity.MovimientoStock> findByTypeAndCreatedAtBetweenOrderByCreatedAtAsc(
            TipoMovimientoStock type, LocalDateTime desde, LocalDateTime hasta);

    long countByProductoId(Long productoId);

    @Query("select coalesce(sum(m.qty), 0) from MovimientoStock m where m.producto.id = :productoId")
    long stockRealDe(@Param("productoId") Long productoId);

    /**
     * Kardex con el stock resultante de cada movimiento.
     *
     * El saldo se calcula en SQL con una suma acumulada, no en Java: asi no
     * depende del orden de los registros ni de que varios movimientos
     * compartan la misma marca de tiempo.
     *
     * Columnas: id, qty, unit_cost, stock_resultante, tipo, motivo,
     *           ref_table, ref_id, lote_id, lot_code, expiry_date, usuario, created_at
     */
    @Query(value = """
            SELECT m.id,
                   m.qty,
                   m.unit_cost,
                   (SELECT COALESCE(SUM(x.qty), 0)
                      FROM stock_movements x
                     WHERE x.product_id = m.product_id
                       AND (x.created_at < m.created_at
                            OR (x.created_at = m.created_at AND x.id <= m.id))
                   ) AS stock_resultante,
                   m.type,
                   m.reason,
                   m.ref_table,
                   m.ref_id,
                   m.lot_id,
                   l.lot_code,
                   l.expiry_date,
                   u.username,
                   m.created_at
              FROM stock_movements m
              LEFT JOIN lots l ON l.id = m.lot_id
              LEFT JOIN users u ON u.id = m.user_id
             WHERE m.product_id = :productoId
             ORDER BY m.created_at DESC, m.id DESC
             LIMIT :limite OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> kardexConSaldo(@Param("productoId") Long productoId,
                                  @Param("offset") int offset,
                                  @Param("limite") int limite);

    @Query(value = "SELECT COUNT(*) FROM stock_movements WHERE product_id = :productoId",
            nativeQuery = true)
    long contarPorProducto(@Param("productoId") Long productoId);
}