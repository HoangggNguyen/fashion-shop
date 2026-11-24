package com.shop.auth.domain.entity;

import com.shop.auth.domain.enums.Role;
import com.shop.auth.domain.enums.UserStatus;
import com.shop.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    @Column(length = 32)
    private String phone;
    @Column(length = 32)
    private String gender;
    private LocalDate birthDate;
    @Column(length = 2048)
    private String avatarUrl;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status;
    @Column(nullable = false)
    private boolean emailVerified;
}
