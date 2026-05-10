package com.tincho.expenses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.tincho.expenses.controllers.dto.CreateIncomeRequest;
import com.tincho.expenses.controllers.dto.CreateTenantRequest;
import com.tincho.expenses.controllers.dto.IncomeResponse;
import com.tincho.expenses.controllers.dto.LoginRequest;
import com.tincho.expenses.controllers.dto.LoginResponse;
import com.tincho.expenses.repositories.IncomeRepository;
import java.math.BigDecimal;
import java.util.List;
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
class IncomeIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

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
    IncomeRepository repository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanup() {
        jdbc.execute("TRUNCATE TABLE income, expense, category, tenant RESTART IDENTITY CASCADE");
    }

    // --- create ---

    @Test
    void create_returns201_andPersistsForAuthenticatedTenant() {
        LoginResponse login = registerAndLogin("Acme", "create@example.com", "plaintext1");

        IncomeResponse created = client.post().uri("/api/incomes")
                .header("Authorization", "Bearer " + login.token())
                .body(new CreateIncomeRequest(new BigDecimal("500.00")))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(IncomeResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.tenantId()).isEqualTo(login.id());
        assertThat(created.amount()).isEqualByComparingTo("500.00");
        assertThat(created.createdAt()).isNotNull();

        var stored = repository.findById(created.id()).orElseThrow();
        assertThat(stored.tenant().getId()).isEqualTo(login.id());
        assertThat(stored.amount()).isEqualByComparingTo("500.00");
    }

    @Test
    void create_returns400_whenAmountMissing() {
        LoginResponse login = registerAndLogin("Acme", "missing-amount@example.com", "plaintext1");

        client.post().uri("/api/incomes")
                .header("Authorization", "Bearer " + login.token())
                .body(new CreateIncomeRequest(null))
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(repository.findByTenantId(login.id())).isEmpty();
    }

    @Test
    void create_returns400_whenAmountNonPositive() {
        LoginResponse login = registerAndLogin("Acme", "neg-amount@example.com", "plaintext1");

        client.post().uri("/api/incomes")
                .header("Authorization", "Bearer " + login.token())
                .body(new CreateIncomeRequest(new BigDecimal("0.00")))
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(repository.findByTenantId(login.id())).isEmpty();
    }

    @Test
    void create_returns401_whenUnauthenticated() {
        client.post().uri("/api/incomes")
                .body(new CreateIncomeRequest(new BigDecimal("100.00")))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void create_returns403_forAdmin() {
        String adminToken = insertAdminAndGetToken();

        client.post().uri("/api/incomes")
                .header("Authorization", "Bearer " + adminToken)
                .body(new CreateIncomeRequest(new BigDecimal("100.00")))
                .exchange()
                .expectStatus().isForbidden();
    }

    // --- list ---

    @Test
    void list_returnsOnlyCallersIncomes_forTenant() {
        LoginResponse a = registerAndLogin("A", "list-a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "list-b@example.com", "plaintext1");
        createIncome(a.token(), "10.00");
        createIncome(a.token(), "20.00");
        createIncome(b.token(), "30.00");

        client.get().uri("/api/incomes")
                .header("Authorization", "Bearer " + a.token())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<IncomeResponse>>() {})
                .value(list -> assertThat(list)
                        .extracting(IncomeResponse::amount)
                        .containsExactlyInAnyOrder(
                                new BigDecimal("10.00"), new BigDecimal("20.00")));
    }

    @Test
    void list_returnsAllIncomes_forAdmin() {
        LoginResponse a = registerAndLogin("A", "list-admin-a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "list-admin-b@example.com", "plaintext1");
        createIncome(a.token(), "10.00");
        createIncome(b.token(), "20.00");
        String adminToken = insertAdminAndGetToken();

        client.get().uri("/api/incomes")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<IncomeResponse>>() {})
                .value(list -> assertThat(list).hasSize(2));
    }

    // --- get ---

    @Test
    void get_returns200_forOwner() {
        LoginResponse login = registerAndLogin("Acme", "get-self@example.com", "plaintext1");
        IncomeResponse created = createIncome(login.token(), "42.00");

        client.get().uri("/api/incomes/{id}", created.id())
                .header("Authorization", "Bearer " + login.token())
                .exchange()
                .expectStatus().isOk()
                .expectBody(IncomeResponse.class)
                .value(i -> assertThat(i.amount()).isEqualByComparingTo("42.00"));
    }

    @Test
    void get_returns404_forTenant_whenOwnedByOther() {
        LoginResponse a = registerAndLogin("A", "get-a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "get-b@example.com", "plaintext1");
        IncomeResponse aInc = createIncome(a.token(), "10.00");

        client.get().uri("/api/incomes/{id}", aInc.id())
                .header("Authorization", "Bearer " + b.token())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void get_returns200_forAdmin_anyIncome() {
        LoginResponse a = registerAndLogin("A", "get-admin-a@example.com", "plaintext1");
        IncomeResponse aInc = createIncome(a.token(), "10.00");
        String adminToken = insertAdminAndGetToken();

        client.get().uri("/api/incomes/{id}", aInc.id())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void get_returns404_whenIncomeMissing() {
        LoginResponse login = registerAndLogin("Acme", "get-missing@example.com", "plaintext1");

        client.get().uri("/api/incomes/{id}", 9999L)
                .header("Authorization", "Bearer " + login.token())
                .exchange()
                .expectStatus().isNotFound();
    }

    // --- delete ---

    @Test
    void delete_returns204_forOwner() {
        LoginResponse login = registerAndLogin("Acme", "del@example.com", "plaintext1");
        IncomeResponse created = createIncome(login.token(), "10.00");

        client.delete().uri("/api/incomes/{id}", created.id())
                .header("Authorization", "Bearer " + login.token())
                .exchange()
                .expectStatus().isNoContent();

        assertThat(repository.findById(created.id())).isEmpty();
    }

    @Test
    void delete_returns404_forTenantWhenOwnedByOther() {
        LoginResponse a = registerAndLogin("A", "del-a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "del-b@example.com", "plaintext1");
        IncomeResponse aInc = createIncome(a.token(), "10.00");

        client.delete().uri("/api/incomes/{id}", aInc.id())
                .header("Authorization", "Bearer " + b.token())
                .exchange()
                .expectStatus().isNotFound();

        assertThat(repository.findById(aInc.id())).isPresent();
    }

    @Test
    void delete_returns204_forAdmin() {
        LoginResponse a = registerAndLogin("A", "del-admin-a@example.com", "plaintext1");
        IncomeResponse aInc = createIncome(a.token(), "10.00");
        String adminToken = insertAdminAndGetToken();

        client.delete().uri("/api/incomes/{id}", aInc.id())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNoContent();

        assertThat(repository.findById(aInc.id())).isEmpty();
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

    private IncomeResponse createIncome(String token, String amount) {
        return client.post().uri("/api/incomes")
                .header("Authorization", "Bearer " + token)
                .body(new CreateIncomeRequest(new BigDecimal(amount)))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(IncomeResponse.class)
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
