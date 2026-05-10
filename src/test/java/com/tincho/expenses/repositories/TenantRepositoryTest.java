package com.tincho.expenses.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tincho.expenses.model.Tenant;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class TenantRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TenantRepository repository;

    @Test
    void save_assignsId() {
        Tenant t = new Tenant(null, "Acme", "save@example.com", "hash", "TENANT", OffsetDateTime.now());

        Tenant saved = repository.save(t);

        assertThat(saved.id()).isNotNull();
    }

    @Test
    void findById_returnsSavedTenant() {
        Tenant saved = repository.save(new Tenant(
                null, "Acme", "findbyid@example.com", "hash", "TENANT", OffsetDateTime.now()));

        Tenant found = repository.findById(saved.id()).orElseThrow();

        assertThat(found.name()).isEqualTo("Acme");
        assertThat(found.email()).isEqualTo("findbyid@example.com");
        assertThat(found.passwordHash()).isEqualTo("hash");
        assertThat(found.role()).isEqualTo("TENANT");
        assertThat(found.createdAt()).isNotNull();
    }

    @Test
    void findByEmail_returnsTenant_whenExists() {
        repository.save(new Tenant(
                null, "Acme", "byemail@example.com", "hash", "TENANT", OffsetDateTime.now()));

        assertThat(repository.findByEmail("byemail@example.com"))
                .isPresent()
                .get()
                .extracting(Tenant::name)
                .isEqualTo("Acme");
    }

    @Test
    void findByEmail_returnsEmpty_whenMissing() {
        assertThat(repository.findByEmail("missing@example.com")).isEmpty();
    }

    @Test
    void existsByEmail_isTrue_whenPresent() {
        repository.save(new Tenant(
                null, "Acme", "exists@example.com", "hash", "TENANT", OffsetDateTime.now()));

        assertThat(repository.existsByEmail("exists@example.com")).isTrue();
    }

    @Test
    void existsByEmail_isFalse_whenAbsent() {
        assertThat(repository.existsByEmail("absent@example.com")).isFalse();
    }

    @Test
    void save_failsOnDuplicateEmail() {
        repository.save(new Tenant(
                null, "First", "dup@example.com", "hash", "TENANT", OffsetDateTime.now()));

        Tenant second = new Tenant(
                null, "Second", "dup@example.com", "hash", "TENANT", OffsetDateTime.now());

        assertThatThrownBy(() -> repository.save(second))
                .isInstanceOf(DataAccessException.class);
    }
}
