package com.chavez.store.api_rest.repository.dao;

import com.chavez.store.api_rest.entity.Producto;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IProductoDAO extends JpaRepository<Producto, Long> {

    // ── Queries derivadas por convencion ──
    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);

    Optional<Producto> findBySku(String sku);

    Optional<Producto> findByBarcode(String barcode);

    List<Producto> findByActiveTrueOrderByNameAsc();

    /**
     * Busqueda con filtros.
     * stockBajo se expresa con dos ramas explicitas en vez de comparar booleanos,
     * porque Hibernate 7 no parsea "(expresion) = :param" dentro de un OR.
     */
    @Query("""
            select p from Producto p
            where p.active = true
              and (:search is null
                   or lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(p.sku) like lower(concat('%', :search, '%'))
                   or (p.barcode is not null and p.barcode like concat('%', :search, '%')))
              and (:categoriaId is null or p.categoria.id = :categoriaId)
              and (:marcaId is null or p.marca.id = :marcaId)
              and (
                    :stockBajo is null
                    or (:stockBajo = true and p.stock <= p.minStock)
                    or (:stockBajo = false and p.stock > p.minStock)
                  )
            """)
    Page<Producto> buscar(@Param("search") String search,
                          @Param("categoriaId") Long categoriaId,
                          @Param("marcaId") Long marcaId,
                          @Param("stockBajo") Boolean stockBajo,
                          Pageable pageable);

    /**
     * Bloqueo pesimista antes de descontar stock.
     * Evita overselling cuando dos ventas concurrenten el mismo producto.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);

    /** Stock en o por debajo del minimo configurado (regla R-I-09). */
    @Query("""
            select p from Producto p
            where p.active = true and p.stock <= p.minStock
            order by p.stock asc
            """)
    List<Producto> findStockCritico();

    /** Productos sin movimientos de venta en los ultimos N dias. */
    @Query("""
            select p from Producto p
            where p.active = true
              and not exists (
                  select sd from DetalleVenta sd
                  where sd.producto = p and sd.venta.status = com.chavez.store.api_rest.entity.enums.EstadoVenta.PAGADA
                    and sd.venta.saleDate >= :desde
              )
            order by p.stock desc
            """)
    List<Producto> findSinVentaReciente(@Param("desde") java.time.LocalDateTime desde);
}