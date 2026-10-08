package com.chavez.store.api_rest.repository.dao;

import com.chavez.store.api_rest.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IUserDAO extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}