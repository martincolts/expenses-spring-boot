package com.tincho.expenses.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tincho.expenses.model.Income;
import com.tincho.expenses.model.Tenant;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class IncomeRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    IncomeRepository repository;

    @Autowired
    TenantRepository tenantRepository;

    private Long createTenant(String email) {
        Tenant t = tenantRepository.save(new Tenant(
                null, "Acme", email, "hash", "TENANT", OffsetDateTime.now()));
        return t.id();
    }

    private Income newIncome(Long tenantId, String amount, OffsetDateTime createdAt) {
        return new Income(
                null,
                AggregateReference.to(tenantId),
                new BigDecimal(amount),
                createdAt);
    }

    @Test
    void save_assignsId() {
        Long tenantId = createTenant("save@example.com");
        Income i = newIncome(tenantId, "100.00", OffsetDateTime.now());

        Income saved = repository.save(i);

        assertThat(saved.id()).isNotNull();
    }

    @Test
    void findById_returnsSavedIncome_withAllFields() {
        Long tenantId = createTenant("findbyid@example.com");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-09T13:00:00Z");
        Income saved = repository.save(newIncome(tenantId, "250.75", createdAt));

        Income found = repository.findById(saved.id()).orElseThrow();

        assertThat(found.tenant().getId()).isEqualTo(tenantId);
        assertThat(found.amount()).isEqualByComparingTo("250.75");
        assertThat(found.createdAt()).isNotNull();
    }

    @Test
    void findByTenantId_returnsOnlyMatchingTenantsIncomes_orderedNewestCreatedFirst() {
        Long tenantA = createTenant("a@example.com");
        Long tenantB = createTenant("b@example.com");
        Income older = repository.save(newIncome(
                tenantA, "100.00", OffsetDateTime.parse("2026-05-01T10:00:00Z")));
        Income newer = repository.save(newIncome(
                tenantA, "200.00", OffsetDateTime.parse("2026-05-09T10:00:00Z")));
        repository.save(newIncome(
                tenantB, "300.00", OffsetDateTime.parse("2026-05-09T10:00:00Z")));

        assertThat(repository.findByTenantId(tenantA))
                .extracting(Income::id)
                .containsExactly(newer.id(), older.id());
    }

    @Test
    void findByTenantId_returnsEmpty_whenNoIncomes() {
        Long tenantId = createTenant("empty@example.com");

        assertThat(repository.findByTenantId(tenantId)).isEmpty();
    }

    @Test
    void findByIdAndTenantId_returnsIncome_whenOwnedByTenant() {
        Long tenantId = createTenant("owner@example.com");
        Income saved = repository.save(newIncome(tenantId, "10.00", OffsetDateTime.now()));

        assertThat(repository.findByIdAndTenantId(saved.id(), tenantId)).isPresent();
    }

    @Test
    void findByIdAndTenantId_returnsEmpty_whenNotOwned() {
        Long tenantA = createTenant("not-owned-a@example.com");
        Long tenantB = createTenant("not-owned-b@example.com");
        Income saved = repository.save(newIncome(tenantA, "10.00", OffsetDateTime.now()));

        assertThat(repository.findByIdAndTenantId(saved.id(), tenantB)).isEmpty();
    }

    @Test
    void save_failsWithUnknownTenant() {
        Income orphan = newIncome(99999L, "10.00", OffsetDateTime.now());

        assertThatThrownBy(() -> repository.save(orphan))
                .isInstanceOf(DataAccessException.class);
    }
}
