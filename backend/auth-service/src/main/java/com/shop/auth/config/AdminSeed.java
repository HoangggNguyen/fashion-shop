package com.shop.auth.config;

import com.shop.auth.domain.entity.User;
import com.shop.auth.domain.enums.Role;
import com.shop.auth.domain.enums.UserStatus;
import com.shop.auth.domain.repository.UserRepository;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class AdminSeed implements ApplicationRunner {
    private final UserRepository users;
    private final String email;
    private final String password;

    public AdminSeed(UserRepository users, @Value("${app.admin.email}") String email,
                     @Value("${app.admin.password}") String password) {
        this.users = users;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByRole(Role.ADMIN)) {
            return;
        }
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new IllegalStateException("ADMIN_EMAIL and ADMIN_PASSWORD must be configured");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (users.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalStateException("ADMIN_EMAIL belongs to an existing non-admin user");
        }
        User admin = new User();
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(new BCryptPasswordEncoder(10).encode(password));
        admin.setFullName("Admin");
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        users.save(admin);
    }
}
