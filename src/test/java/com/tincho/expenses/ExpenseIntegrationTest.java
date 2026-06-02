package com.tincho.expenses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.tincho.expenses.controllers.dto.CategoryResponse;
import com.tincho.expenses.controllers.dto.CreateCategoryRequest;
import com.tincho.expenses.controllers.dto.CreateExpenseRequest;
import com.tincho.expenses.controllers.dto.CreateTenantRequest;
import com.tincho.expenses.controllers.dto.ExpenseResponse;
import com.tincho.expenses.controllers.dto.LoginRequest;
import com.tincho.expenses.controllers.dto.LoginResponse;
import com.tincho.expenses.controllers.dto.PageResponse;
import com.tincho.expenses.controllers.dto.UpdateExpenseRequest;
import com.tincho.expenses.model.ExpenseStatus;
import com.tincho.expenses.repositories.ExpenseRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureRestTestClient
@Testcontainers
class ExpenseIntegrationTest {

    private static final OffsetDateTime INCURRED_AT =
            OffsetDateTime.parse("2026-05-09T13:00:00Z");

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
    RestTestClient client;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ExpenseRepository repository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanup() {
        jdbc.execute("TRUNCATE TABLE expense, category, tenant RESTART IDENTITY CASCADE");
    }

    // --- create ---

    @Test
    void create_returns201_andPersistsAsPending_whenPaidFlagFalse() {
        LoginResponse login = registerAndLogin("Acme", "create@example.com", "plaintext1");
        CategoryResponse category = createCategory(login.token(), "Food");

        ExpenseResponse created = client.post().uri("/api/expenses")
                .header("Authorization", "Bearer " + login.token())
                .body(new CreateExpenseRequest(
                        category.id(), "Lunch",
                        new BigDecimal("12.50"), "usd", INCURRED_AT, false))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ExpenseResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.tenantId()).isEqualTo(login.id());
        assertThat(created.categoryId()).isEqualTo(category.id());
        assertThat(created.description()).isEqualTo("Lunch");
        assertThat(created.amount()).isEqualByComparingTo("12.50");
        assertThat(created.currency()).isEqualTo("USD");
        assertThat(created.status()).isEqualTo(ExpenseStatus.NOT_PAUD_YET);
        assertThat(created.paidAt()).isNull();

        var stored = repository.findById(created.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(ExpenseStatus.NOT_PAUD_YET);
    }

    @Test
    void create_returns201_andPersistsAsPaid_whenPaidFlagTrue() {
        LoginResponse login = registerAndLogin("Acme", "paid@example.com", "plaintext1");
        CategoryResponse category = createCategory(login.token(), "Food");

        ExpenseResponse created = client.post().uri("/api/expenses")
                .header("Authorization", "Bearer " + login.token())
                .body(new CreateExpenseRequest(
                        category.id(), "Lunch",
                        new BigDecimal("12.50"), "USD", INCURRED_AT, true))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ExpenseResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created.status()).isEqualTo(ExpenseStatus.PAID);
        assertThat(created.paidAt()).isNotNull();
    }

    @Test
    void create_returns404_whenCategoryBelongsToOtherTenant() {
        LoginResponse a = registerAndLogin("A", "create-a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "create-b@example.com", "plaintext1");
        CategoryResponse aCategory = createCategory(a.token(), "A-Food");

        client.post().uri("/api/expenses")
                .header("Authorization", "Bearer " + b.token())
                .body(new CreateExpenseRequest(
                        aCategory.id(), "Lunch",
                        new BigDecimal("12.50"), "USD", INCURRED_AT, false))
                .exchange()
                .expectStatus().isNotFound();

        assertThat(repository.findByTenantId(b.id())).isEmpty();
    }

    @Test
    void create_returns400_whenCategoryIdMissing() {
        LoginResponse login = registerAndLogin("Acme", "missing-cat@example.com", "plaintext1");

        client.post().uri("/api/expenses")
                .header("Authorization", "Bearer " + login.token())
                .body(new CreateExpenseRequest(
                        null, "Lunch",
                        new BigDecimal("12.50"), "USD", INCURRED_AT, false))
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(repository.findByTenantId(login.id())).isEmpty();
    }

    @Test
    void create_returns400_whenAmountNonPositive() {
        LoginResponse login = registerAndLogin("Acme", "neg-amount@example.com", "plaintext1");
        CategoryResponse category = createCategory(login.token(), "Food");

        client.post().uri("/api/expenses")
                .header("Authorization", "Bearer " + login.token())
                .body(new CreateExpenseRequest(
                        category.id(), "Lunch",
                        new BigDecimal("0.00"), "USD", INCURRED_AT, false))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void create_returns400_whenCurrencyWrongLength() {
        LoginResponse login = registerAndLogin("Acme", "currency@example.com", "plaintext1");
        CategoryResponse category = createCategory(login.token(), "Food");

        client.post().uri("/api/expenses")
                .header("Authorization", "Bearer " + login.token())
                .body(new CreateExpenseRequest(
                        category.id(), "Lunch",
                        new BigDecimal("10.00"), "DOLLARS", INCURRED_AT, false))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void create_returns401_whenUnauthenticated() {
        client.post().uri("/api/expenses")
                .body(new CreateExpenseRequest(
                        1L, "Lunch", new BigDecimal("10.00"), "USD", INCURRED_AT, false))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void create_returns403_forAdmin() {
        String adminToken = insertAdminAndGetToken();

        client.post().uri("/api/expenses")
                .header("Authorization", "Bearer " + adminToken)
                .body(new CreateExpenseRequest(
                        1L, "Lunch", new BigDecimal("10.00"), "USD", INCURRED_AT, false))
                .exchange()
                .expectStatus().isForbidden();
    }

    // --- list ---

    @Test
    void list_returnsOnlyCallersExpenses_forTenant() {
        LoginResponse a = registerAndLogin("A", "list-a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "list-b@example.com", "plaintext1");
        CategoryResponse catA = createCategory(a.token(), "A-Food");
        CategoryResponse catB = createCategory(b.token(), "B-Food");
        createExpense(a.token(), catA.id(), "A-Lunch", false);
        createExpense(a.token(), catA.id(), "A-Dinner", true);
        createExpense(b.token(), catB.id(), "B-Lunch", false);

        client.get().uri("/api/expenses")
                .header("Authorization", "Bearer " + a.token())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PageResponse<ExpenseResponse>>() {})
                .value(page -> assertThat(page.content())
                        .extracting(ExpenseResponse::description)
                        .containsExactlyInAnyOrder("A-Lunch", "A-Dinner"));
    }

    @Test
    void list_filtersByCategory() {
        LoginResponse login = registerAndLogin("Acme", "filter-cat@example.com", "plaintext1");
        CategoryResponse food = createCategory(login.token(), "Food");
        CategoryResponse travel = createCategory(login.token(), "Travel");
        createExpense(login.token(), food.id(), "Lunch", false);
        createExpense(login.token(), travel.id(), "Flight", false);

        client.get().uri(u -> u.path("/api/expenses")
                        .queryParam("categoryId", food.id()).build())
                .header("Authorization", "Bearer " + login.token())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PageResponse<ExpenseResponse>>() {})
                .value(page -> assertThat(page.content())
                        .extracting(ExpenseResponse::description)
                        .containsExactly("Lunch"));
    }

    @Test
    void list_filtersByStatus() {
        LoginResponse login = registerAndLogin("Acme", "filter-status@example.com", "plaintext1");
        CategoryResponse cat = createCategory(login.token(), "Food");
        createExpense(login.token(), cat.id(), "Paid expense", true);
        createExpense(login.token(), cat.id(), "Pending expense", false);

        client.get().uri(u -> u.path("/api/expenses")
                        .queryParam("status", "PAID").build())
                .header("Authorization", "Bearer " + login.token())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PageResponse<ExpenseResponse>>() {})
                .value(page -> assertThat(page.content())
                        .extracting(ExpenseResponse::description)
                        .containsExactly("Paid expense"));
    }

    @Test
    void list_filtersByDateRange() {
        LoginResponse login = registerAndLogin("Acme", "filter-date@example.com", "plaintext1");
        CategoryResponse cat = createCategory(login.token(), "Food");

        client.post().uri("/api/expenses")
                .header("Authorization", "Bearer " + login.token())
                .body(new CreateExpenseRequest(cat.id(), "January",
                        new BigDecimal("10.00"), "USD",
                        OffsetDateTime.parse("2026-01-15T10:00:00Z"), false))
                .exchange().expectStatus().isCreated();

        client.post().uri("/api/expenses")
                .header("Authorization", "Bearer " + login.token())
                .body(new CreateExpenseRequest(cat.id(), "May",
                        new BigDecimal("10.00"), "USD",
                        OffsetDateTime.parse("2026-05-15T10:00:00Z"), false))
                .exchange().expectStatus().isCreated();

        client.get().uri(u -> u.path("/api/expenses")
                        .queryParam("dateFrom", "2026-05-01T00:00:00Z")
                        .queryParam("dateTo", "2026-05-31T23:59:59Z")
                        .build())
                .header("Authorization", "Bearer " + login.token())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PageResponse<ExpenseResponse>>() {})
                .value(page -> assertThat(page.content())
                        .extracting(ExpenseResponse::description)
                        .containsExactly("May"));
    }

    @Test
    void list_combinesMultipleFilters() {
        LoginResponse login = registerAndLogin("Acme", "filter-combined@example.com", "plaintext1");
        CategoryResponse food = createCategory(login.token(), "Food");
        CategoryResponse travel = createCategory(login.token(), "Travel");
        createExpense(login.token(), food.id(), "Food paid", true);
        createExpense(login.token(), food.id(), "Food pending", false);
        createExpense(login.token(), travel.id(), "Travel paid", true);

        client.get().uri(u -> u.path("/api/expenses")
                        .queryParam("categoryId", food.id())
                        .queryParam("status", "PAID")
                        .build())
                .header("Authorization", "Bearer " + login.token())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PageResponse<ExpenseResponse>>() {})
                .value(page -> assertThat(page.content())
                        .extracting(ExpenseResponse::description)
                        .containsExactly("Food paid"));
    }

    @Test
    void list_returnsAllExpenses_forAdmin() {
        LoginResponse a = registerAndLogin("A", "list-admin-a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "list-admin-b@example.com", "plaintext1");
        CategoryResponse catA = createCategory(a.token(), "A-Food");
        CategoryResponse catB = createCategory(b.token(), "B-Food");
        createExpense(a.token(), catA.id(), "A-Lunch", false);
        createExpense(b.token(), catB.id(), "B-Lunch", false);
        String adminToken = insertAdminAndGetToken();

        client.get().uri("/api/expenses")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PageResponse<ExpenseResponse>>() {})
                .value(page -> assertThat(page.content()).hasSize(2));
    }

    // --- get ---

    @Test
    void get_returns200_forOwner() {
        LoginResponse login = registerAndLogin("Acme", "get-self@example.com", "plaintext1");
        CategoryResponse category = createCategory(login.token(), "Food");
        ExpenseResponse created = createExpense(login.token(), category.id(), "Lunch", false);

        client.get().uri("/api/expenses/{id}", created.id())
                .header("Authorization", "Bearer " + login.token())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ExpenseResponse.class)
                .value(e -> assertThat(e.description()).isEqualTo("Lunch"));
    }

    @Test
    void get_returns404_forTenant_whenOwnedByOther() {
        LoginResponse a = registerAndLogin("A", "get-a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "get-b@example.com", "plaintext1");
        CategoryResponse catA = createCategory(a.token(), "A-Food");
        ExpenseResponse aExp = createExpense(a.token(), catA.id(), "A-Lunch", false);

        client.get().uri("/api/expenses/{id}", aExp.id())
                .header("Authorization", "Bearer " + b.token())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void get_returns200_forAdmin_anyExpense() {
        LoginResponse a = registerAndLogin("A", "get-admin-a@example.com", "plaintext1");
        CategoryResponse catA = createCategory(a.token(), "A-Food");
        ExpenseResponse aExp = createExpense(a.token(), catA.id(), "A-Lunch", false);
        String adminToken = insertAdminAndGetToken();

        client.get().uri("/api/expenses/{id}", aExp.id())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk();
    }

    // --- update ---

    @Test
    void update_returns200_andPersistsEditableFields_andPreservesStatus() {
        LoginResponse login = registerAndLogin("Acme", "upd@example.com", "plaintext1");
        CategoryResponse oldCat = createCategory(login.token(), "Food");
        CategoryResponse newCat = createCategory(login.token(), "Travel");
        ExpenseResponse created = createExpense(login.token(), oldCat.id(), "Old", true);

        OffsetDateTime newIncurred = OffsetDateTime.parse("2026-06-01T10:00:00Z");
        client.put().uri("/api/expenses/{id}", created.id())
                .header("Authorization", "Bearer " + login.token())
                .body(new UpdateExpenseRequest(
                        newCat.id(), "New", new BigDecimal("99.99"), "EUR", newIncurred))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ExpenseResponse.class)
                .value(e -> {
                    assertThat(e.categoryId()).isEqualTo(newCat.id());
                    assertThat(e.description()).isEqualTo("New");
                    assertThat(e.amount()).isEqualByComparingTo("99.99");
                    assertThat(e.currency()).isEqualTo("EUR");
                    assertThat(e.incurredAt()).isEqualTo(newIncurred);
                    assertThat(e.status()).isEqualTo(ExpenseStatus.PAID);
                    assertThat(e.paidAt()).isNotNull();
                });

        var stored = repository.findById(created.id()).orElseThrow();
        assertThat(stored.description()).isEqualTo("New");
    }

    @Test
    void update_returns404_whenCategoryBelongsToOtherTenant() {
        LoginResponse a = registerAndLogin("A", "upd-a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "upd-b@example.com", "plaintext1");
        CategoryResponse catA = createCategory(a.token(), "A-Food");
        CategoryResponse catB = createCategory(b.token(), "B-Food");
        ExpenseResponse aExp = createExpense(a.token(), catA.id(), "Lunch", false);

        client.put().uri("/api/expenses/{id}", aExp.id())
                .header("Authorization", "Bearer " + a.token())
                .body(new UpdateExpenseRequest(
                        catB.id(), "x", new BigDecimal("1.00"), "USD", INCURRED_AT))
                .exchange()
                .expectStatus().isNotFound();
    }

    // --- pay ---

    @Test
    void pay_returns200_andTransitionsToPaid() {
        LoginResponse login = registerAndLogin("Acme", "pay@example.com", "plaintext1");
        CategoryResponse category = createCategory(login.token(), "Food");
        ExpenseResponse created = createExpense(login.token(), category.id(), "Lunch", false);

        client.post().uri("/api/expenses/{id}/pay", created.id())
                .header("Authorization", "Bearer " + login.token())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ExpenseResponse.class)
                .value(e -> {
                    assertThat(e.status()).isEqualTo(ExpenseStatus.PAID);
                    assertThat(e.paidAt()).isNotNull();
                });

        var stored = repository.findById(created.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(ExpenseStatus.PAID);
        assertThat(stored.paidAt()).isNotNull();
    }

    @Test
    void pay_returns409_whenAlreadyPaid() {
        LoginResponse login = registerAndLogin("Acme", "pay-dup@example.com", "plaintext1");
        CategoryResponse category = createCategory(login.token(), "Food");
        ExpenseResponse created = createExpense(login.token(), category.id(), "Lunch", true);

        client.post().uri("/api/expenses/{id}/pay", created.id())
                .header("Authorization", "Bearer " + login.token())
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void pay_returns404_forTenant_whenOwnedByOther() {
        LoginResponse a = registerAndLogin("A", "pay-a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "pay-b@example.com", "plaintext1");
        CategoryResponse catA = createCategory(a.token(), "A-Food");
        ExpenseResponse aExp = createExpense(a.token(), catA.id(), "Lunch", false);

        client.post().uri("/api/expenses/{id}/pay", aExp.id())
                .header("Authorization", "Bearer " + b.token())
                .exchange()
                .expectStatus().isNotFound();
    }

    // --- unpay ---

    @Test
    void unpay_returns200_andTransitionsToPending_andClearsPaidAt() {
        LoginResponse login = registerAndLogin("Acme", "unpay@example.com", "plaintext1");
        CategoryResponse category = createCategory(login.token(), "Food");
        ExpenseResponse created = createExpense(login.token(), category.id(), "Lunch", true);

        client.post().uri("/api/expenses/{id}/unpay", created.id())
                .header("Authorization", "Bearer " + login.token())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ExpenseResponse.class)
                .value(e -> {
                    assertThat(e.status()).isEqualTo(ExpenseStatus.NOT_PAUD_YET);
                    assertThat(e.paidAt()).isNull();
                });

        var stored = repository.findById(created.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(ExpenseStatus.NOT_PAUD_YET);
        assertThat(stored.paidAt()).isNull();
    }

    @Test
    void unpay_returns409_whenAlreadyNotPaid() {
        LoginResponse login = registerAndLogin("Acme", "unpay-dup@example.com", "plaintext1");
        CategoryResponse category = createCategory(login.token(), "Food");
        ExpenseResponse created = createExpense(login.token(), category.id(), "Lunch", false);

        client.post().uri("/api/expenses/{id}/unpay", created.id())
                .header("Authorization", "Bearer " + login.token())
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void unpay_returns404_forTenant_whenOwnedByOther() {
        LoginResponse a = registerAndLogin("A", "unpay-a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "unpay-b@example.com", "plaintext1");
        CategoryResponse catA = createCategory(a.token(), "A-Food");
        ExpenseResponse aExp = createExpense(a.token(), catA.id(), "Lunch", true);

        client.post().uri("/api/expenses/{id}/unpay", aExp.id())
                .header("Authorization", "Bearer " + b.token())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void unpay_returns200_forAdmin() {
        LoginResponse a = registerAndLogin("A", "unpay-admin@example.com", "plaintext1");
        CategoryResponse catA = createCategory(a.token(), "A-Food");
        ExpenseResponse aExp = createExpense(a.token(), catA.id(), "Lunch", true);
        String adminToken = insertAdminAndGetToken();

        client.post().uri("/api/expenses/{id}/unpay", aExp.id())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk();
    }

    // --- delete ---

    @Test
    void delete_returns204_forOwner() {
        LoginResponse login = registerAndLogin("Acme", "del@example.com", "plaintext1");
        CategoryResponse category = createCategory(login.token(), "Food");
        ExpenseResponse created = createExpense(login.token(), category.id(), "Lunch", false);

        client.delete().uri("/api/expenses/{id}", created.id())
                .header("Authorization", "Bearer " + login.token())
                .exchange()
                .expectStatus().isNoContent();

        assertThat(repository.findById(created.id())).isEmpty();
    }

    @Test
    void delete_returns404_forTenantWhenOwnedByOther() {
        LoginResponse a = registerAndLogin("A", "del-a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "del-b@example.com", "plaintext1");
        CategoryResponse catA = createCategory(a.token(), "A-Food");
        ExpenseResponse aExp = createExpense(a.token(), catA.id(), "Lunch", false);

        client.delete().uri("/api/expenses/{id}", aExp.id())
                .header("Authorization", "Bearer " + b.token())
                .exchange()
                .expectStatus().isNotFound();

        assertThat(repository.findById(aExp.id())).isPresent();
    }

    // --- Helpers ---

    private LoginResponse registerAndLogin(String name, String email, String password) {
        return client.post().uri("/api/auth/register")
                .body(new CreateTenantRequest(name, email, password))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private CategoryResponse createCategory(String token, String name) {
        return client.post().uri("/api/categories")
                .header("Authorization", "Bearer " + token)
                .body(new CreateCategoryRequest(name, null, null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CategoryResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private ExpenseResponse createExpense(String token, Long categoryId, String description, boolean paid) {
        return client.post().uri("/api/expenses")
                .header("Authorization", "Bearer " + token)
                .body(new CreateExpenseRequest(
                        categoryId, description, new BigDecimal("10.00"),
                        "USD", INCURRED_AT, paid))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ExpenseResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private String insertAdminAndGetToken() {
        String hash = passwordEncoder.encode("adminpass1");
        jdbc.update(
                "INSERT INTO tenant (name, email, password_hash, role) VALUES ('Admin', 'admin@example.com', ?, 'ADMIN')",
                hash);
        return client.post().uri("/api/auth/login")
                .body(new LoginRequest("admin@example.com", "adminpass1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody()
                .token();
    }
}
