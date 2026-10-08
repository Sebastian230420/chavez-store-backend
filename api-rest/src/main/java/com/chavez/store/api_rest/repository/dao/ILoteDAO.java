package com.chavez.store.api_rest.repository.dao;

import com.chavez.store.api_rest.entity.Lote;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ILoteDAO extends JpaRepository<Lote, Long> {

    Optional<Lote> findByLotCode(String lotCode);

    boolean existsByLotCode(String lotCode);

    /** Lote generado al recibir una compra: se localiza por su detalle de origen. */
    Optional<Lote> findByDetalleCompraId(Long detalleCompraId);

    long countByProductoId(Long productoId);

    /**
     * FEFO: lotes del producto ordenados por vencimiento mas proximo primero.
     * Los lotes sin vencimiento se consumen al final (expiryDate null last).
     * Solo lotes con saldo disponible. Bloqueo pesimista para evitar doble consumo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select l from Lote l
            where l.producto.id = :productoId
              and l.active = true
              and l.qtyRemaining > 0
            order by l.expiryDate asc nulls last, l.id asc
            """)
    List<Lote> findLotesFEFO(@Param("productoId") Long productoId);

    /** Lotes que vencen dentro del rango indicado (reporte de stock por vencer). */
    @Query("""
            select l from Lote l
            where l.qtyRemaining > 0
              and l.expiryDate is not null
              and l.expiryDate between :desde and :hasta
            order by l.expiryDate asc
            """)
    List<Lote> findLotesPorVencer(@Param("desde") LocalDate desde,
                                  @Param("hasta") LocalDate hasta);

    /** Lotes ya vencidos con saldo: candidatos naturales a merma. */
    @Query("""
            select l from Lote l
            where l.qtyRemaining > 0 and l.expiryDate is not null
              and l.expiryDate < :hoy
            order by l.expiryDate asc
            """)
    List<Lote> findLotesVencidos(@Param("hoy") LocalDate hoy);

    List<Lote> findByProductoIdAndQtyRemainingGreaterThanOrderByExpiryDateAsc(
            Long productoId, Integer qtyMin);
}