package com.chavez.store.api_rest.repository.dao;

import com.chavez.store.api_rest.entity.Venta;
import com.chavez.store.api_rest.entity.enums.EstadoVenta;
import com.chavez.store.api_rest.entity.enums.TipoVenta;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IVentaDAO extends JpaRepository<Venta, Long> {

    Optional<Venta> findByDocument(String document);

    boolean existsByDocument(String document);

    @Query("""
            select v from Venta v
            where (:status is null or v.status = :status)
              and (:tipo is null or v.type = :tipo)
              and (:clienteId is null or v.cliente.id = :clienteId)
              and (:desde is null or v.saleDate >= :desde)
              and (:hasta is null or v.saleDate <= :hasta)
            """)
    Page<Venta> buscar(@Param("status") EstadoVenta status,
                       @Param("tipo") TipoVenta tipo,
                       @Param("clienteId") Long clienteId,
                       @Param("desde") LocalDateTime desde,
                       @Param("hasta") LocalDateTime hasta,
                       Pageable pageable);

    /** Ventas a credito vigentes de un cliente: base del saldo deudor. */
    @Query("""
            select v from Venta v
            where v.cliente.id = :clienteId
              and v.type = com.chavez.store.api_rest.entity.enums.TipoVenta.CREDITO
              and v.status = com.chavez.store.api_rest.entity.enums.EstadoVenta.PAGADA
            order by v.saleDate asc
            """)
    List<Venta> findCreditoVigentesDe(@Param("clienteId") Long clienteId);

    List<Venta> findByClienteIdAndTypeAndStatusOrderBySaleDateDesc(
            Long clienteId, TipoVenta type, EstadoVenta status);
}