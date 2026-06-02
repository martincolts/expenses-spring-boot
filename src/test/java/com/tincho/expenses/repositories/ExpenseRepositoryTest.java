package com.tincho.expenses.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tincho.expenses.model.Category;
import com.tincho.expenses.model.Expense;
import com.tincho.expenses.model.ExpenseStatus;
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
class ExpenseRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withUrlParam("stringtype", "unspecified");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    ExpenseRepository repository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    TenantRepository tenantRepository;

    private Long createTenant(String email) {
        Tenant t = tenantRepository.save(new Tenant(
                null, "Acme", email, "hash", "TENANT", OffsetDateTime.now()));
        return t.id();
    }

    private Long createCategory(Long tenantId, String name) {
        Category c = categoryRepository.save(new Category(
                null, AggregateReference.to(tenantId), name, null, null, OffsetDateTime.now()));
        return c.id();
    }

    private Expense newExpense(Long tenantId, Long categoryId, ExpenseStatus status,
                               OffsetDateTime incurredAt) {
        return new Expense(
                null,
                AggregateReference.to(tenantId),
                AggregateReference.to(categoryId),
                "desc", new BigDecimal("10.00"), "USD",
                status,
                incurredAt,
                OffsetDateTime.now(),
                status == ExpenseStatus.PAID ? OffsetDateTime.now() : null);
    }

    @Test
    void save_assignsId() {
        Long tenantId = createTenant("save@example.com");
        Long categoryId = createCategory(tenantId, "Food");
        Expense e = newExpense(tenantId, categoryId, ExpenseStatus.PENDING,
                OffsetDateTime.parse("2026-05-09T13:00:00Z"));

        Expense saved = repository.save(e);

        assertThat(saved.id()).isNotNull();
    }

    @Test
    void findById_returnsSavedExpense_withAllFields() {
        Long tenantId = createTenant("findbyid@example.com");
        Long categoryId = createCategory(tenantId, "Food");
        OffsetDateTime incurredAt = OffsetDateTime.parse("2026-05-09T13:00:00Z");
        Expense saved = repository.save(newExpense(
                tenantId, categoryId, ExpenseStatus.PAID, incurredAt));

        Expense found = repository.findById(saved.id()).orElseThrow();

        assertThat(found.tenant().getId()).isEqualTo(tenantId);
        assertThat(found.category().getId()).isEqualTo(categoryId);
        assertThat(found.description()).isEqualTo("desc");
        assertThat(found.amount()).isEqualByComparingTo("10.00");
        assertThat(found.currency()).isEqualTo("USD");
        assertThat(found.status()).isEqualTo(ExpenseStatus.PAID);
        assertThat(found.paidAt()).isNotNull();
        assertThat(found.incurredAt()).isNotNull();
        assertThat(found.createdAt()).isNotNull();
    }

    @Test
    void save_persistsBothEnumValues() {
        Long tenantId = createTenant("enums@example.com");
        Long categoryId = createCategory(tenantId, "Food");
        Expense paid = repository.save(newExpense(tenantId, categoryId, ExpenseStatus.PAID,
                OffsetDateTime.parse("2026-05-09T13:00:00Z")));
        Expense pending = repository.save(newExpense(tenantId, categoryId, ExpenseStatus.PENDING,
                OffsetDateTime.parse("2026-05-09T13:00:00Z")));

        assertThat(repository.findById(paid.id()).orElseThrow().status())
                .isEqualTo(ExpenseStatus.PAID);
        assertThat(repository.findById(pending.id()).orElseThrow().status())
                .isEqualTo(ExpenseStatus.PENDING);
    }

    @Test
    void findByTenantId_returnsOnlyMatchingTenantsExpenses_orderedNewestIncurredFirst() {
        Long tenantA = createTenant("a@example.com");
        Long tenantB = createTenant("b@example.com");
        Long catA = createCategory(tenantA, "Food");
        Long catB = createCategory(tenantB, "Food");
        Expense older = repository.save(newExpense(tenantA, catA, ExpenseStatus.PAID,
                OffsetDateTime.parse("2026-05-01T10:00:00Z")));
        Expense newer = repository.save(newExpense(tenantA, catA, ExpenseStatus.PENDING,
                OffsetDateTime.parse("2026-05-09T10:00:00Z")));
        repository.save(newExpense(tenantB, catB, ExpenseStatus.PAID,
                OffsetDateTime.parse("2026-05-09T10:00:00Z")));

        assertThat(repository.findByTenantId(tenantA))
                .extracting(Expense::id)
                .containsExactly(newer.id(), older.id());
    }

    @Test
    void findByTenantId_returnsEmpty_whenNoExpenses() {
        Long tenantId = createTenant("empty@example.com");

        assertThat(repository.findByTenantId(tenantId)).isEmpty();
    }

    @Test
    void findByIdAndTenantId_returnsExpense_whenOwnedByTenant() {
        Long tenantId = createTenant("owner@example.com");
        Long categoryId = createCategory(tenantId, "Food");
        Expense saved = repository.save(newExpense(tenantId, categoryId, ExpenseStatus.PAID,
                OffsetDateTime.parse("2026-05-09T13:00:00Z")));

        assertThat(repository.findByIdAndTenantId(saved.id(), tenantId)).isPresent();
    }

    @Test
    void findByIdAndTenantId_returnsEmpty_whenNotOwned() {
        Long tenantA = createTenant("not-owned-a@example.com");
        Long tenantB = createTenant("not-owned-b@example.com");
        Long categoryA = createCategory(tenantA, "Food");
        Expense saved = repository.save(newExpense(tenantA, categoryA, ExpenseStatus.PAID,
                OffsetDateTime.parse("2026-05-09T13:00:00Z")));

        assertThat(repository.findByIdAndTenantId(saved.id(), tenantB)).isEmpty();
    }

    @Test
    void save_failsWithUnknownTenant() {
        Expense orphan = newExpense(99999L, 99999L, ExpenseStatus.PAID,
                OffsetDateTime.parse("2026-05-09T13:00:00Z"));

        assertThatThrownBy(() -> repository.save(orphan))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void findByTenantIdFilteredPaged_filtersByCategory() {
        Long tenantId = createTenant("cat-filter@example.com");
        Long catA = createCategory(tenantId, "Food");
        Long catB = createCategory(tenantId, "Travel");
        Expense food = repository.save(newExpense(tenantId, catA, ExpenseStatus.PENDING,
                OffsetDateTime.parse("2026-05-01T10:00:00Z")));
        repository.save(newExpense(tenantId, catB, ExpenseStatus.PENDING,
                OffsetDateTime.parse("2026-05-02T10:00:00Z")));

        var results = repository.findByTenantIdFilteredPaged(tenantId, catA, null, null, null, 20, 0L);

        assertThat(results).extracting(Expense::id).containsExactly(food.id());
    }

    @Test
    void findByTenantIdFilteredPaged_filtersByStatus() {
        Long tenantId = createTenant("status-filter@example.com");
        Long cat = createCategory(tenantId, "Food");
        Expense paid = repository.save(newExpense(tenantId, cat, ExpenseStatus.PAID,
                OffsetDateTime.parse("2026-05-01T10:00:00Z")));
        repository.save(newExpense(tenantId, cat, ExpenseStatus.PENDING,
                OffsetDateTime.parse("2026-05-02T10:00:00Z")));

        var results = repository.findByTenantIdFilteredPaged(tenantId, null, "PAID", null, null, 20, 0L);

        assertThat(results).extracting(Expense::id).containsExactly(paid.id());
    }

    @Test
    void findByTenantIdFilteredPaged_filtersByDateRange() {
        Long tenantId = createTenant("date-filter@example.com");
        Long cat = createCategory(tenantId, "Food");
        repository.save(newExpense(tenantId, cat, ExpenseStatus.PENDING,
                OffsetDateTime.parse("2026-03-01T10:00:00Z")));
        Expense inRange = repository.save(newExpense(tenantId, cat, ExpenseStatus.PENDING,
                OffsetDateTime.parse("2026-05-15T10:00:00Z")));
        repository.save(newExpense(tenantId, cat, ExpenseStatus.PENDING,
                OffsetDateTime.parse("2026-08-01T10:00:00Z")));

        var results = repository.findByTenantIdFilteredPaged(
                tenantId, null, null,
                OffsetDateTime.parse("2026-05-01T00:00:00Z"),
                OffsetDateTime.parse("2026-05-31T23:59:59Z"),
                20, 0L);

        assertThat(results).extracting(Expense::id).containsExactly(inRange.id());
    }

    @Test
    void findByTenantIdFilteredPaged_noFilters_returnsAll() {
        Long tenantId = createTenant("no-filter@example.com");
        Long cat = createCategory(tenantId, "Food");
        repository.save(newExpense(tenantId, cat, ExpenseStatus.PAID,
                OffsetDateTime.parse("2026-05-01T10:00:00Z")));
        repository.save(newExpense(tenantId, cat, ExpenseStatus.PENDING,
                OffsetDateTime.parse("2026-05-09T10:00:00Z")));

        var results = repository.findByTenantIdFilteredPaged(tenantId, null, null, null, null, 20, 0L);

        assertThat(results).hasSize(2);
    }

    @Test
    void countByTenantIdFiltered_matchesFilteredResults() {
        Long tenantId = createTenant("count-filter@example.com");
        Long cat = createCategory(tenantId, "Food");
        repository.save(newExpense(tenantId, cat, ExpenseStatus.PAID,
                OffsetDateTime.parse("2026-05-01T10:00:00Z")));
        repository.save(newExpense(tenantId, cat, ExpenseStatus.PENDING,
                OffsetDateTime.parse("2026-05-09T10:00:00Z")));

        long count = repository.countByTenantIdFiltered(tenantId, null, "PAID", null, null);

        assertThat(count).isEqualTo(1L);
    }

    @Test
    void findAllFilteredPaged_returnsAcrossAllTenants() {
        Long tenantA = createTenant("all-a@example.com");
        Long tenantB = createTenant("all-b@example.com");
        Long catA = createCategory(tenantA, "Food");
        Long catB = createCategory(tenantB, "Food");
        Expense expA = repository.save(newExpense(tenantA, catA, ExpenseStatus.PAID,
                OffsetDateTime.parse("2026-05-01T10:00:00Z")));
        Expense expB = repository.save(newExpense(tenantB, catB, ExpenseStatus.PAID,
                OffsetDateTime.parse("2026-05-02T10:00:00Z")));

        var results = repository.findAllFilteredPaged(null, "PAID", null, null, 20, 0L);

        assertThat(results).extracting(Expense::id).containsExactlyInAnyOrder(expA.id(), expB.id());
    }
}
