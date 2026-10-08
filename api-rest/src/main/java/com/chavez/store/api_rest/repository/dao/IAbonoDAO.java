package com.chavez.store.api_rest.repository.dao;

import com.chavez.store.api_rest.entity.Abono;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IAbonoDAO extends JpaRepository<Abono, Long> {

    List<Abono> findByClienteIdOrderByCreatedAtDesc(Long clienteId);

    Page<Abono> findByClienteIdOrderByCreatedAtDesc(Long clienteId, Pageable pageable);

    List<Abono> findByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime desde, LocalDateTime hasta);

    List<Abono> findByClienteIdAndAnnulledAtIsNull(Long clienteId);

    /** Total abonado por un cliente, ignorando abonos anulados. */
    @Query("""
            select coalesce(sum(a.amount), 0) from Abono a
            where a.cliente.id = :clienteId and a.annulledAt is null
            """)
    BigDecimal totalAbonado(@Param("clienteId") Long clienteId);

    /** Total de abonos no anulados en un periodo (reporte de recaudado). */
    @Query("""
            select coalesce(sum(a.amount), 0) from Abono a
            where a.annulledAt is null
              and a.createdAt between :desde and :hasta
            """)
    BigDecimal recaudadoEntre(@Param("desde") LocalDateTime desde,
                              @Param("hasta") LocalDateTime hasta);
}