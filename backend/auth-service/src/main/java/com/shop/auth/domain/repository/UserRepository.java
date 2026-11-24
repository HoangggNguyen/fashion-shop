package com.shop.auth.domain.repository;

import com.shop.auth.domain.entity.User;
import com.shop.auth.domain.enums.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByRole(Role role);
}
