package com.tincho.expenses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.tincho.expenses.controllers.dto.CreateTenantRequest;
import com.tincho.expenses.controllers.dto.LoginRequest;
import com.tincho.expenses.controllers.dto.LoginResponse;
import com.tincho.expenses.controllers.dto.ResetPasswordRequest;
import com.tincho.expenses.controllers.dto.TenantResponse;
import com.tincho.expenses.controllers.dto.UpdateTenantRequest;
import com.tincho.expenses.repositories.TenantRepository;
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
class TenantIntegrationTest {

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
    TenantRepository repository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanup() {
        jdbc.execute("TRUNCATE TABLE tenant RESTART IDENTITY CASCADE");
    }

    // --- Auth: register ---

    @Test
    void register_returns201_andPersistsEncodedPassword() {
        CreateTenantRequest req = new CreateTenantRequest("Acme", "create@example.com", "plaintext1");

        LoginResponse body = client.post().uri("/api/auth/register")
                .body(req)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.name()).isEqualTo("Acme");
        assertThat(body.email()).isEqualTo("create@example.com");
        assertThat(body.role()).isEqualTo("TENANT");
        assertThat(body.token()).isNotBlank();

        var stored = repository.findById(body.id()).orElseThrow();
        assertThat(stored.passwordHash()).isNotEqualTo("plaintext1");
        assertThat(passwordEncoder.matches("plaintext1", stored.passwordHash())).isTrue();
    }

    @Test
    void register_returns409_whenEmailAlreadyExists() {
        client.post().uri("/api/auth/register")
                .body(new CreateTenantRequest("First", "dup@example.com", "plaintext1"))
                .exchange()
                .expectStatus().isCreated();

        client.post().uri("/api/auth/register")
                .body(new CreateTenantRequest("Second", "dup@example.com", "plaintext1"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(String.class)
                .value(b -> assertThat(b).contains("dup@example.com"));
    }

    @Test
    void register_returns400_whenValidationFails() {
        CreateTenantRequest invalid = new CreateTenantRequest("", "not-an-email", "short");

        client.post().uri("/api/auth/register")
                .body(invalid)
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(repository.findByEmail("not-an-email")).isEmpty();
    }

    // --- Auth: login ---

    @Test
    void login_returns200_withToken() {
        registerAndGetToken("Acme", "login@example.com", "mysecret1");

        LoginResponse response = client.post().uri("/api/auth/login")
                .body(new LoginRequest("login@example.com", "mysecret1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.token()).isNotBlank();
        assertThat(response.email()).isEqualTo("login@example.com");
    }

    @Test
    void login_returns401_whenPasswordWrong() {
        registerAndGetToken("Acme", "login-bad@example.com", "correctpass");

        client.post().uri("/api/auth/login")
                .body(new LoginRequest("login-bad@example.com", "wrongpass1"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void login_returns401_whenUserNotFound() {
        client.post().uri("/api/auth/login")
                .body(new LoginRequest("missing@example.com", "anypass12"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // --- Auth: reset-password ---

    @Test
    void resetPassword_returns204_andNewPasswordWorks() {
        String token = registerAndGetToken("Acme", "reset@example.com", "oldpass12");

        client.post().uri("/api/auth/reset-password")
                .header("Authorization", "Bearer " + token)
                .body(new ResetPasswordRequest("oldpass12", "newpass12"))
                .exchange()
                .expectStatus().isNoContent();

        client.post().uri("/api/auth/login")
                .body(new LoginRequest("reset@example.com", "newpass12"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void resetPassword_returns422_whenCurrentPasswordWrong() {
        String token = registerAndGetToken("Acme", "reset-bad@example.com", "correctpass");

        client.post().uri("/api/auth/reset-password")
                .header("Authorization", "Bearer " + token)
                .body(new ResetPasswordRequest("wrongpass1", "newpass12"))
                .exchange()
                .expectStatus().isEqualTo(400);
    }

    @Test
    void resetPassword_returns401_whenUnauthenticated() {
        client.post().uri("/api/auth/reset-password")
                .body(new ResetPasswordRequest("oldpass12", "newpass12"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // --- Tenant CRUD: list ---

    @Test
    void list_returns200_forAdmin() {
        String adminToken = insertAdminAndGetToken();
        registerAndGetToken("A", "list-a@example.com", "plaintext1");
        registerAndGetToken("B", "list-b@example.com", "plaintext1");

        client.get().uri("/api/tenants")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<TenantResponse>>() {})
                .value(list -> assertThat(list).hasSizeGreaterThanOrEqualTo(2));
    }

    @Test
    void list_returns403_forTenant() {
        String token = registerAndGetToken("A", "list-tenant@example.com", "plaintext1");

        client.get().uri("/api/tenants")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void list_returns401_whenUnauthenticated() {
        client.get().uri("/api/tenants")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // --- Tenant CRUD: get ---

    @Test
    void get_returns200_forTenantWithSameId() {
        LoginResponse login = registerAndLogin("Acme", "get-self@example.com", "plaintext1");

        client.get().uri("/api/tenants/{id}", login.id())
                .header("Authorization", "Bearer " + login.token())
                .exchange()
                .expectStatus().isOk()
                .expectBody(TenantResponse.class)
                .value(t -> assertThat(t.email()).isEqualTo("get-self@example.com"));
    }

    @Test
    void get_returns403_forTenantWithDifferentId() {
        LoginResponse tenantA = registerAndLogin("A", "get-a@example.com", "plaintext1");
        LoginResponse tenantB = registerAndLogin("B", "get-b@example.com", "plaintext1");

        client.get().uri("/api/tenants/{id}", tenantA.id())
                .header("Authorization", "Bearer " + tenantB.token())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void get_returns404_forAdmin_whenMissing() {
        String adminToken = insertAdminAndGetToken();

        client.get().uri("/api/tenants/{id}", 9999L)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .value(b -> assertThat(b).contains("9999"));
    }

    // --- Tenant CRUD: update ---

    @Test
    void update_returns200_forTenantWithSameId() {
        LoginResponse login = registerAndLogin("Old", "update-self@example.com", "plaintext1");

        client.put().uri("/api/tenants/{id}", login.id())
                .header("Authorization", "Bearer " + login.token())
                .body(new UpdateTenantRequest("New"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TenantResponse.class)
                .value(t -> {
                    assertThat(t.name()).isEqualTo("New");
                    assertThat(t.email()).isEqualTo("update-self@example.com");
                });
    }

    @Test
    void update_returns403_forTenantWithDifferentId() {
        LoginResponse tenantA = registerAndLogin("A", "update-a@example.com", "plaintext1");
        LoginResponse tenantB = registerAndLogin("B", "update-b@example.com", "plaintext1");

        client.put().uri("/api/tenants/{id}", tenantA.id())
                .header("Authorization", "Bearer " + tenantB.token())
                .body(new UpdateTenantRequest("Hack"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void update_returns400_whenNameBlank() {
        LoginResponse login = registerAndLogin("Old", "update-blank@example.com", "plaintext1");

        client.put().uri("/api/tenants/{id}", login.id())
                .header("Authorization", "Bearer " + login.token())
                .body(new UpdateTenantRequest(""))
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(repository.findById(login.id()).orElseThrow().name()).isEqualTo("Old");
    }

    // --- Tenant CRUD: delete ---

    @Test
    void delete_returns204_forAdmin() {
        LoginResponse login = registerAndLogin("ToDelete", "delete@example.com", "plaintext1");
        String adminToken = insertAdminAndGetToken();

        client.delete().uri("/api/tenants/{id}", login.id())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNoContent();

        assertThat(repository.findById(login.id())).isEmpty();
    }

    @Test
    void delete_returns403_forTenant() {
        LoginResponse login = registerAndLogin("Acme", "delete-tenant@example.com", "plaintext1");

        client.delete().uri("/api/tenants/{id}", login.id())
                .header("Authorization", "Bearer " + login.token())
                .exchange()
                .expectStatus().isForbidden();
    }

    // --- Helpers ---

    private String registerAndGetToken(String name, String email, String password) {
        return registerAndLogin(name, email, password).token();
    }

    private LoginResponse registerAndLogin(String name, String email, String password) {
        return client.post().uri("/api/auth/register")
                .body(new CreateTenantRequest(name, email, password))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(LoginResponse.class)
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
