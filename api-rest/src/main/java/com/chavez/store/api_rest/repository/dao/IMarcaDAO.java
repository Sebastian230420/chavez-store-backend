package com.chavez.store.api_rest.repository.dao;

import com.chavez.store.api_rest.entity.Marca;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IMarcaDAO extends JpaRepository<Marca, Long> {

    Optional<Marca> findByNameIgnoreCase(String name);

    List<Marca> findByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}