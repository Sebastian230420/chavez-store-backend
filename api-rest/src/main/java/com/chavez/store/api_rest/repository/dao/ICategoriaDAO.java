package com.chavez.store.api_rest.repository.dao;

import com.chavez.store.api_rest.entity.Categoria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICategoriaDAO extends JpaRepository<Categoria, Long> {

    Optional<Categoria> findByNameIgnoreCase(String name);

    List<Categoria> findByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}