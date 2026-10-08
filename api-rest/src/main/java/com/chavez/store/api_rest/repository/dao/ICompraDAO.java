package com.chavez.store.api_rest.repository.dao;

import com.chavez.store.api_rest.entity.Compra;
import com.chavez.store.api_rest.entity.enums.EstadoCompra;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICompraDAO extends JpaRepository<Compra, Long> {

    Optional<Compra> findByDocument(String document);

    boolean existsByDocument(String document);

    Page<Compra> findByStatusOrderByIssueDateDescIdDesc(EstadoCompra status, Pageable pageable);

    List<Compra> findByProveedorIdAndStatusOrderByIssueDateDesc(Long proveedorId, EstadoCompra status);

    Page<Compra> findByIssueDateBetweenOrderByIssueDateDesc(LocalDate desde, LocalDate hasta, Pageable pageable);
}