package com.chavez.store.api_rest.repository.dao;

import com.chavez.store.api_rest.entity.Presentacion;
import com.chavez.store.api_rest.entity.enums.TipoPresentacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPresentacionDAO extends JpaRepository<Presentacion, Long> {

    List<Presentacion> findByProductoIdOrderBySortOrderAscIdAsc(Long productoId);

    List<Presentacion> findByProductoIdAndTypeAndActiveTrue(Long productoId, TipoPresentacion type);

    Optional<Presentacion> findByIdAndProductoId(Long id, Long productoId);

    long countByProductoIdAndType(Long productoId, TipoPresentacion type);

    void deleteByProductoId(Long productoId);
}