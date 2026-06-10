package com.ecommerce.project.repositories;

import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByUserName(String username);

    boolean existsByUserName(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleName = :role")
    Page<User> findByRoleName(@Param("role") AppRole appRole, Pageable pageable);

    @EntityGraph(attributePaths = "roles")
    @Query("SELECT DISTINCT u FROM User u WHERE u.userId IN :userIds")
    List<User> findUsersWithRoles(List<Long> userIds);
}
