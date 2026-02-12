package com.mario.backend.rbac.repository;

import com.mario.backend.rbac.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(String name);

    List<Permission> findByNameIn(List<String> names);

    List<Permission> findByService(String service);

    boolean existsByName(String name);
}
