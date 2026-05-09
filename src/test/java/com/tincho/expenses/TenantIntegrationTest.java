package com.tincho.expenses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.tincho.expenses.controllers.dto.CreateTenantRequest;
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

    @Test
    void create_returns201_andPersistsEncodedPassword() {
        CreateTenantRequest req = new CreateTenantRequest("Acme", "create@example.com", "plaintext");

        TenantResponse body = client.post().uri("/api/tenants")
                .body(req)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TenantResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.name()).isEqualTo("Acme");
        assertThat(body.email()).isEqualTo("create@example.com");
        assertThat(body.createdAt()).isNotNull();

        var stored = repository.findById(body.id()).orElseThrow();
        assertThat(stored.passwordHash()).isNotEqualTo("plaintext");
        assertThat(passwordEncoder.matches("plaintext", stored.passwordHash())).isTrue();
    }

    @Test
    void create_returns409_whenEmailAlreadyExists() {
        client.post().uri("/api/tenants")
                .body(new CreateTenantRequest("First", "dup@example.com", "plaintext"))
                .exchange()
                .expectStatus().isCreated();

        client.post().uri("/api/tenants")
                .body(new CreateTenantRequest("Second", "dup@example.com", "plaintext"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(String.class)
                .value(b -> assertThat(b).contains("dup@example.com"));
    }

    @Test
    void create_returns400_whenValidationFails() {
        CreateTenantRequest invalid = new CreateTenantRequest("", "not-an-email", "short");

        client.post().uri("/api/tenants")
                .body(invalid)
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(repository.findByEmail("not-an-email")).isEmpty();
    }

    @Test
    void list_returnsAllTenants() {
        createTenant("A", "list-a@example.com");
        createTenant("B", "list-b@example.com");

        client.get().uri("/api/tenants")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<TenantResponse>>() {})
                .value(list -> assertThat(list)
                        .extracting(TenantResponse::email)
                        .containsExactlyInAnyOrder("list-a@example.com", "list-b@example.com"));
    }

    @Test
    void list_returnsEmpty_whenNoTenants() {
        client.get().uri("/api/tenants")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<TenantResponse>>() {})
                .value(list -> assertThat(list).isEmpty());
    }

    @Test
    void get_returnsTenant() {
        TenantResponse created = createTenant("Acme", "get@example.com");

        client.get().uri("/api/tenants/{id}", created.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(TenantResponse.class)
                .value(t -> assertThat(t).isEqualTo(created));
    }

    @Test
    void get_returns404_whenMissing() {
        client.get().uri("/api/tenants/{id}", 9999L)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .value(b -> assertThat(b).contains("9999"));
    }

    @Test
    void update_changesName_andPreservesEmailAndPassword() {
        TenantResponse created = createTenant("Old", "update@example.com");
        String originalHash = repository.findById(created.id()).orElseThrow().passwordHash();

        client.put().uri("/api/tenants/{id}", created.id())
                .body(new UpdateTenantRequest("New"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TenantResponse.class)
                .value(t -> {
                    assertThat(t.name()).isEqualTo("New");
                    assertThat(t.email()).isEqualTo("update@example.com");
                });

        var stored = repository.findById(created.id()).orElseThrow();
        assertThat(stored.name()).isEqualTo("New");
        assertThat(stored.email()).isEqualTo("update@example.com");
        assertThat(stored.passwordHash()).isEqualTo(originalHash);
    }

    @Test
    void update_returns400_whenNameBlank() {
        TenantResponse created = createTenant("Old", "update-blank@example.com");

        client.put().uri("/api/tenants/{id}", created.id())
                .body(new UpdateTenantRequest(""))
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(repository.findById(created.id()).orElseThrow().name()).isEqualTo("Old");
    }

    @Test
    void update_returns404_whenMissing() {
        client.put().uri("/api/tenants/{id}", 9999L)
                .body(new UpdateTenantRequest("New"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .value(b -> assertThat(b).contains("9999"));
    }

    @Test
    void delete_removesTenant_andReturns204() {
        TenantResponse created = createTenant("Acme", "delete@example.com");

        client.delete().uri("/api/tenants/{id}", created.id())
                .exchange()
                .expectStatus().isNoContent();

        assertThat(repository.findById(created.id())).isEmpty();
    }

    @Test
    void delete_returns404_whenMissing() {
        client.delete().uri("/api/tenants/{id}", 9999L)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .value(b -> assertThat(b).contains("9999"));
    }

    private TenantResponse createTenant(String name, String email) {
        return client.post().uri("/api/tenants")
                .body(new CreateTenantRequest(name, email, "plaintext"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TenantResponse.class)
                .returnResult()
                .getResponseBody();
    }
}
