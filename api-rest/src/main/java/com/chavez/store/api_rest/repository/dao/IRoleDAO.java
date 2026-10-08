package com.chavez.store.api_rest.repository.dao;

import com.chavez.store.api_rest.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IRoleDAO extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);
}