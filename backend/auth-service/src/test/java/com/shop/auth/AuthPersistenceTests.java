package com.shop.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shop.auth.config.AdminSeed;
import com.shop.auth.domain.entity.Address;
import com.shop.auth.domain.entity.PasswordResetToken;
import com.shop.auth.domain.entity.RefreshToken;
import com.shop.auth.domain.enums.Role;
import com.shop.auth.domain.enums.UserStatus;
import com.shop.auth.domain.repository.AddressRepository;
import com.shop.auth.domain.repository.PasswordResetTokenRepository;
import com.shop.auth.domain.repository.RefreshTokenRepository;
import com.shop.auth.domain.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuthPersistenceTests {
    @Autowired private UserRepository users;
    @Autowired private AddressRepository addresses;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private PasswordResetTokenRepository resetTokens;
    @Autowired private AdminSeed seed;

    @Test
    void migrationAndSeedCreateAnAuditedAdminOnlyOnce() throws Exception {
        var admin = users.findByEmailIgnoreCase("ADMIN@EXAMPLE.TEST").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(admin.getCreatedAt()).isNotNull();
        assertThat(admin.getUpdatedAt()).isNotNull();
        assertThat(new BCryptPasswordEncoder(10).matches("test-password-123", admin.getPasswordHash())).isTrue();
        long count = users.count();
        seed.run(new DefaultApplicationArguments(new String[0]));
        assertThat(users.count()).isEqualTo(count);
    }

    @Test
    void migrationPersistsRelatedEntitiesWithAuditing() {
        var admin = users.findByEmailIgnoreCase("admin@example.test").orElseThrow();
        var address = new Address();
        address.setUser(admin);
        address.setReceiverName("Test");
        address.setPhone("123");
        address.setProvince("Province");
        address.setDistrict("District");
        address.setWard("Ward");
        address.setStreet("Street");
        assertThat(addresses.saveAndFlush(address).getCreatedAt()).isNotNull();

        var refresh = new RefreshToken();
        refresh.setUser(admin);
        refresh.setTokenHash("refresh-hash");
        refresh.setExpiresAt(Instant.now().plusSeconds(60));
        assertThat(refreshTokens.saveAndFlush(refresh).getId()).isNotNull();

        var reset = new PasswordResetToken();
        reset.setUser(admin);
        reset.setTokenHash("reset-hash");
        reset.setExpiresAt(Instant.now().plusSeconds(60));
        assertThat(resetTokens.saveAndFlush(reset).getId()).isNotNull();
    }

    @Test
    void refusesToPromoteAnExistingCustomer() {
        var seedForCustomer = new AdminSeed(users, "customer@example.test", "test-password-123");
        var user = new com.shop.auth.domain.entity.User();
        user.setEmail("customer@example.test");
        user.setPasswordHash("hash");
        user.setFullName("Customer");
        user.setRole(Role.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        users.deleteAll();
        users.saveAndFlush(user);
        assertThatThrownBy(() -> seedForCustomer.run(new DefaultApplicationArguments(new String[0])))
            .isInstanceOf(IllegalStateException.class);
    }
}
