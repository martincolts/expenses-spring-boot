package com.tincho.expenses.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.tincho.expenses.model.PasswordResetToken;
import com.tincho.expenses.model.Tenant;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class PasswordResetTokenRepositoryTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    PasswordResetTokenRepository repository;

    @Autowired
    TenantRepository tenantRepository;

    private Tenant aTenant(String email) {
        return tenantRepository.save(new Tenant(
                null, "Acme", email, "hash", "TENANT", OffsetDateTime.now()));
    }

    private PasswordResetToken aToken(Long tenantId, String token, OffsetDateTime usedAt) {
        OffsetDateTime now = OffsetDateTime.now();
        return repository.save(new PasswordResetToken(
                null, AggregateReference.to(tenantId), token,
                "http://localhost/reset-password?token=" + token,
                now.plusHours(1), usedAt, now));
    }

    @Test
    void findByToken_returnsSavedToken() {
        Tenant tenant = aTenant("findtoken@example.com");
        aToken(tenant.id(), "tok-123", null);

        assertThat(repository.findByToken("tok-123"))
                .isPresent()
                .get()
                .satisfies(t -> {
                    assertThat(t.tenant().getId()).isEqualTo(tenant.id());
                    assertThat(t.usedAt()).isNull();
                    assertThat(t.expiresAt()).isNotNull();
                });
    }

    @Test
    void findByToken_returnsEmpty_whenMissing() {
        assertThat(repository.findByToken("nope")).isEmpty();
    }

    @Test
    void invalidateActiveForTenant_marksUnusedTokensUsed() {
        Tenant tenant = aTenant("invalidate@example.com");
        aToken(tenant.id(), "active-1", null);
        aToken(tenant.id(), "active-2", null);

        repository.invalidateActiveForTenant(tenant.id());

        assertThat(repository.findByToken("active-1").orElseThrow().usedAt()).isNotNull();
        assertThat(repository.findByToken("active-2").orElseThrow().usedAt()).isNotNull();
    }

    @Test
    void invalidateActiveForTenant_doesNotTouchOtherTenants() {
        Tenant target = aTenant("target@example.com");
        Tenant other = aTenant("other@example.com");
        aToken(target.id(), "target-tok", null);
        aToken(other.id(), "other-tok", null);

        repository.invalidateActiveForTenant(target.id());

        assertThat(repository.findByToken("other-tok").orElseThrow().usedAt()).isNull();
    }
}
