package com.chavez.store.api_rest.repository.dao;

import com.chavez.store.api_rest.entity.Cliente;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IClienteDAO extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByDocument(String document);

    boolean existsByDocument(String document);

    List<Cliente> findByActiveTrueOrderByFullNameAsc();

    @Query("""
            select c from Cliente c
            where c.active = true
              and (:search is null
                   or lower(c.fullName) like lower(concat('%', :search, '%'))
                   or c.document like concat('%', :search, '%'))
              and (:tipo is null or c.type = :tipo)
            """)
    Page<Cliente> buscar(@Param("search") String search,
                         @Param("tipo") com.chavez.store.api_rest.entity.enums.TipoCliente tipo,
                         Pageable pageable);

    /**
     * Saldo deudor = SUM(ventas CREDITO PAGADAS) - SUM(abonos no anulados).
     * Invariante 5 de MODELO_DATOS.md.
     */
    @Query("""
            select coalesce(
                (select sum(v.total) from Venta v
                  where v.cliente.id = :clienteId
                    and v.type = com.chavez.store.api_rest.entity.enums.TipoVenta.CREDITO
                    and v.status = com.chavez.store.api_rest.entity.enums.EstadoVenta.PAGADA)
                , 0)
            -
            coalesce(
                (select sum(a.amount) from Abono a
                  where a.cliente.id = :clienteId and a.annulledAt is null)
                , 0)
            """)
    BigDecimal saldoDeudor(@Param("clienteId") Long clienteId);

    /** Clientes con saldo por cobrar, ordenados por monto. */
    @Query("""
            select c from Cliente c
            where c.active = true
            order by c.fullName asc
            """)
    List<Cliente> findActivos();
}