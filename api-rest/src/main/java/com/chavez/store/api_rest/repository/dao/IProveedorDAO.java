package com.chavez.store.api_rest.repository.dao;

import com.chavez.store.api_rest.entity.Proveedor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IProveedorDAO extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByDocument(String document);

    boolean existsByDocument(String document);

    List<Proveedor> findByActiveTrueOrderByNameAsc();

    List<Proveedor> findByNameContainingIgnoreCaseOrderByNameAsc(String search);
}