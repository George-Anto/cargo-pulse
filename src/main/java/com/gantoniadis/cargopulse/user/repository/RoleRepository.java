package com.gantoniadis.cargopulse.user.repository;

import com.gantoniadis.cargopulse.user.entity.Role;
import com.gantoniadis.cargopulse.user.util.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Finds a list of Role entities whose 'name' matches any value in the provided Set of UserRole enums.
     */
    List<Role> findByNameIn(Set<UserRole> names);
}
