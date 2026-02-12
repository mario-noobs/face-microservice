package com.mario.backend.rbac.repository;

import com.mario.backend.rbac.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    Optional<Role> findByIsDefaultTrue();

    boolean existsByName(String name);

    long countByName(String name);
}
