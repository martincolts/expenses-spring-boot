package com.tincho.expenses.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tincho.expenses.model.Category;
import com.tincho.expenses.model.Tenant;
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
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class CategoryRepositoryTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    CategoryRepository repository;

    @Autowired
    TenantRepository tenantRepository;

    private Long createTenant(String email) {
        Tenant t = tenantRepository.save(new Tenant(
                null, "Acme", email, "hash", "TENANT", OffsetDateTime.now()));
        return t.id();
    }

    @Test
    void save_assignsId() {
        Long tenantId = createTenant("save@example.com");
        Category c = new Category(
                null, AggregateReference.to(tenantId), "Food", null, null, OffsetDateTime.now());

        Category saved = repository.save(c);

        assertThat(saved.id()).isNotNull();
    }

    @Test
    void findById_returnsSavedCategory() {
        Long tenantId = createTenant("findbyid@example.com");
        Category saved = repository.save(new Category(
                null, AggregateReference.to(tenantId), "Travel", null, null, OffsetDateTime.now()));

        Category found = repository.findById(saved.id()).orElseThrow();

        assertThat(found.name()).isEqualTo("Travel");
        assertThat(found.tenant().getId()).isEqualTo(tenantId);
        assertThat(found.createdAt()).isNotNull();
    }

    @Test
    void findByTenantId_returnsOnlyMatchingTenantsCategories() {
        Long tenantA = createTenant("tenant-a@example.com");
        Long tenantB = createTenant("tenant-b@example.com");
        repository.save(new Category(
                null, AggregateReference.to(tenantA), "A-Food", null, null, OffsetDateTime.now()));
        repository.save(new Category(
                null, AggregateReference.to(tenantA), "A-Travel", null, null, OffsetDateTime.now()));
        repository.save(new Category(
                null, AggregateReference.to(tenantB), "B-Food", null, null, OffsetDateTime.now()));

        assertThat(repository.findByTenantId(tenantA))
                .extracting(Category::name)
                .containsExactlyInAnyOrder("A-Food", "A-Travel");
    }

    @Test
    void findByTenantId_returnsEmpty_whenNoCategories() {
        Long tenantId = createTenant("empty@example.com");

        assertThat(repository.findByTenantId(tenantId)).isEmpty();
    }

    @Test
    void findByIdAndTenantId_returnsCategory_whenOwnedByTenant() {
        Long tenantId = createTenant("owner@example.com");
        Category saved = repository.save(new Category(
                null, AggregateReference.to(tenantId), "Food", null, null, OffsetDateTime.now()));

        assertThat(repository.findByIdAndTenantId(saved.id(), tenantId))
                .isPresent()
                .get()
                .extracting(Category::name)
                .isEqualTo("Food");
    }

    @Test
    void findByIdAndTenantId_returnsEmpty_whenNotOwned() {
        Long tenantA = createTenant("a@example.com");
        Long tenantB = createTenant("b@example.com");
        Category saved = repository.save(new Category(
                null, AggregateReference.to(tenantA), "Food", null, null, OffsetDateTime.now()));

        assertThat(repository.findByIdAndTenantId(saved.id(), tenantB)).isEmpty();
    }

    @Test
    void existsByTenantIdAndName_isTrue_whenPresent() {
        Long tenantId = createTenant("exists@example.com");
        repository.save(new Category(
                null, AggregateReference.to(tenantId), "Food", null, null, OffsetDateTime.now()));

        assertThat(repository.existsByTenantIdAndName(tenantId, "Food")).isTrue();
    }

    @Test
    void existsByTenantIdAndName_isFalse_whenSameNameButDifferentTenant() {
        Long tenantA = createTenant("a-other@example.com");
        Long tenantB = createTenant("b-other@example.com");
        repository.save(new Category(
                null, AggregateReference.to(tenantA), "Food", null, null, OffsetDateTime.now()));

        assertThat(repository.existsByTenantIdAndName(tenantB, "Food")).isFalse();
    }

    @Test
    void save_failsOnDuplicateNameForSameTenant() {
        Long tenantId = createTenant("dup@example.com");
        repository.save(new Category(
                null, AggregateReference.to(tenantId), "Food", null, null, OffsetDateTime.now()));

        Category second = new Category(
                null, AggregateReference.to(tenantId), "Food", null, null, OffsetDateTime.now());

        assertThatThrownBy(() -> repository.save(second))
                .isInstanceOf(DataAccessException.class);
    }
}
