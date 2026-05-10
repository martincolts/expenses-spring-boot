package com.tincho.expenses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.tincho.expenses.controllers.dto.CategoryResponse;
import com.tincho.expenses.controllers.dto.CreateCategoryRequest;
import com.tincho.expenses.controllers.dto.CreateTenantRequest;
import com.tincho.expenses.controllers.dto.LoginRequest;
import com.tincho.expenses.controllers.dto.LoginResponse;
import com.tincho.expenses.controllers.dto.UpdateCategoryRequest;
import com.tincho.expenses.repositories.CategoryRepository;
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
class CategoryIntegrationTest {

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
    CategoryRepository repository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanup() {
        jdbc.execute("TRUNCATE TABLE category, tenant RESTART IDENTITY CASCADE");
    }

    // --- create ---

    @Test
    void create_returns201_andPersistsForAuthenticatedTenant() {
        LoginResponse login = registerAndLogin("Acme", "create@example.com", "plaintext1");

        CategoryResponse created = client.post().uri("/api/categories")
                .header("Authorization", "Bearer " + login.token())
                .body(new CreateCategoryRequest("Food"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CategoryResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.tenantId()).isEqualTo(login.id());
        assertThat(created.name()).isEqualTo("Food");

        var stored = repository.findById(created.id()).orElseThrow();
        assertThat(stored.tenant().getId()).isEqualTo(login.id());
    }

    @Test
    void create_returns409_whenNameAlreadyExistsForTenant() {
        LoginResponse login = registerAndLogin("Acme", "dup@example.com", "plaintext1");

        client.post().uri("/api/categories")
                .header("Authorization", "Bearer " + login.token())
                .body(new CreateCategoryRequest("Food"))
                .exchange()
                .expectStatus().isCreated();

        client.post().uri("/api/categories")
                .header("Authorization", "Bearer " + login.token())
                .body(new CreateCategoryRequest("Food"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(String.class)
                .value(b -> assertThat(b).contains("Food"));
    }

    @Test
    void create_returns201_whenSameNameForDifferentTenant() {
        LoginResponse a = registerAndLogin("A", "a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "b@example.com", "plaintext1");

        client.post().uri("/api/categories")
                .header("Authorization", "Bearer " + a.token())
                .body(new CreateCategoryRequest("Food"))
                .exchange()
                .expectStatus().isCreated();

        client.post().uri("/api/categories")
                .header("Authorization", "Bearer " + b.token())
                .body(new CreateCategoryRequest("Food"))
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void create_returns400_whenNameBlank() {
        LoginResponse login = registerAndLogin("Acme", "blank@example.com", "plaintext1");

        client.post().uri("/api/categories")
                .header("Authorization", "Bearer " + login.token())
                .body(new CreateCategoryRequest(""))
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(repository.findByTenantId(login.id())).isEmpty();
    }

    @Test
    void create_returns401_whenUnauthenticated() {
        client.post().uri("/api/categories")
                .body(new CreateCategoryRequest("Food"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void create_returns403_forAdmin() {
        String adminToken = insertAdminAndGetToken();

        client.post().uri("/api/categories")
                .header("Authorization", "Bearer " + adminToken)
                .body(new CreateCategoryRequest("Food"))
                .exchange()
                .expectStatus().isForbidden();
    }

    // --- list ---

    @Test
    void list_returnsOnlyCallersCategories_forTenant() {
        LoginResponse a = registerAndLogin("A", "list-a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "list-b@example.com", "plaintext1");
        createCategory(a.token(), "A-Food");
        createCategory(a.token(), "A-Travel");
        createCategory(b.token(), "B-Food");

        client.get().uri("/api/categories")
                .header("Authorization", "Bearer " + a.token())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<CategoryResponse>>() {})
                .value(list -> assertThat(list)
                        .extracting(CategoryResponse::name)
                        .containsExactlyInAnyOrder("A-Food", "A-Travel"));
    }

    @Test
    void list_returnsAllCategories_forAdmin() {
        LoginResponse a = registerAndLogin("A", "list-admin-a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "list-admin-b@example.com", "plaintext1");
        createCategory(a.token(), "A-Food");
        createCategory(b.token(), "B-Food");
        String adminToken = insertAdminAndGetToken();

        client.get().uri("/api/categories")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<CategoryResponse>>() {})
                .value(list -> assertThat(list).hasSize(2));
    }

    @Test
    void list_returns401_whenUnauthenticated() {
        client.get().uri("/api/categories")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // --- get ---

    @Test
    void get_returns200_forOwner() {
        LoginResponse login = registerAndLogin("Acme", "get-self@example.com", "plaintext1");
        CategoryResponse created = createCategory(login.token(), "Food");

        client.get().uri("/api/categories/{id}", created.id())
                .header("Authorization", "Bearer " + login.token())
                .exchange()
                .expectStatus().isOk()
                .expectBody(CategoryResponse.class)
                .value(c -> assertThat(c.name()).isEqualTo("Food"));
    }

    @Test
    void get_returns404_forTenantWhenOwnedByOther() {
        LoginResponse a = registerAndLogin("A", "get-a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "get-b@example.com", "plaintext1");
        CategoryResponse aCat = createCategory(a.token(), "A-Food");

        client.get().uri("/api/categories/{id}", aCat.id())
                .header("Authorization", "Bearer " + b.token())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void get_returns200_forAdmin_anyCategory() {
        LoginResponse a = registerAndLogin("A", "get-admin-a@example.com", "plaintext1");
        CategoryResponse aCat = createCategory(a.token(), "A-Food");
        String adminToken = insertAdminAndGetToken();

        client.get().uri("/api/categories/{id}", aCat.id())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CategoryResponse.class)
                .value(c -> assertThat(c.tenantId()).isEqualTo(a.id()));
    }

    @Test
    void get_returns404_forAdmin_whenMissing() {
        String adminToken = insertAdminAndGetToken();

        client.get().uri("/api/categories/{id}", 9999L)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    // --- update ---

    @Test
    void update_returns200_forOwner_andPersistsName() {
        LoginResponse login = registerAndLogin("Acme", "upd@example.com", "plaintext1");
        CategoryResponse created = createCategory(login.token(), "Old");

        client.put().uri("/api/categories/{id}", created.id())
                .header("Authorization", "Bearer " + login.token())
                .body(new UpdateCategoryRequest("New"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(CategoryResponse.class)
                .value(c -> assertThat(c.name()).isEqualTo("New"));

        assertThat(repository.findById(created.id()).orElseThrow().name()).isEqualTo("New");
    }

    @Test
    void update_returns404_forTenantWhenOwnedByOther() {
        LoginResponse a = registerAndLogin("A", "upd-a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "upd-b@example.com", "plaintext1");
        CategoryResponse aCat = createCategory(a.token(), "A-Food");

        client.put().uri("/api/categories/{id}", aCat.id())
                .header("Authorization", "Bearer " + b.token())
                .body(new UpdateCategoryRequest("Hack"))
                .exchange()
                .expectStatus().isNotFound();

        assertThat(repository.findById(aCat.id()).orElseThrow().name()).isEqualTo("A-Food");
    }

    @Test
    void update_returns409_whenNameAlreadyExistsForTenant() {
        LoginResponse login = registerAndLogin("Acme", "upd-conf@example.com", "plaintext1");
        createCategory(login.token(), "Taken");
        CategoryResponse other = createCategory(login.token(), "Other");

        client.put().uri("/api/categories/{id}", other.id())
                .header("Authorization", "Bearer " + login.token())
                .body(new UpdateCategoryRequest("Taken"))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void update_returns400_whenNameBlank() {
        LoginResponse login = registerAndLogin("Acme", "upd-blank@example.com", "plaintext1");
        CategoryResponse created = createCategory(login.token(), "Old");

        client.put().uri("/api/categories/{id}", created.id())
                .header("Authorization", "Bearer " + login.token())
                .body(new UpdateCategoryRequest(""))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // --- delete ---

    @Test
    void delete_returns204_forOwner() {
        LoginResponse login = registerAndLogin("Acme", "del@example.com", "plaintext1");
        CategoryResponse created = createCategory(login.token(), "Food");

        client.delete().uri("/api/categories/{id}", created.id())
                .header("Authorization", "Bearer " + login.token())
                .exchange()
                .expectStatus().isNoContent();

        assertThat(repository.findById(created.id())).isEmpty();
    }

    @Test
    void delete_returns404_forTenantWhenOwnedByOther() {
        LoginResponse a = registerAndLogin("A", "del-a@example.com", "plaintext1");
        LoginResponse b = registerAndLogin("B", "del-b@example.com", "plaintext1");
        CategoryResponse aCat = createCategory(a.token(), "A-Food");

        client.delete().uri("/api/categories/{id}", aCat.id())
                .header("Authorization", "Bearer " + b.token())
                .exchange()
                .expectStatus().isNotFound();

        assertThat(repository.findById(aCat.id())).isPresent();
    }

    @Test
    void delete_returns204_forAdmin() {
        LoginResponse a = registerAndLogin("A", "del-admin-a@example.com", "plaintext1");
        CategoryResponse aCat = createCategory(a.token(), "A-Food");
        String adminToken = insertAdminAndGetToken();

        client.delete().uri("/api/categories/{id}", aCat.id())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNoContent();

        assertThat(repository.findById(aCat.id())).isEmpty();
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
                .body(new CreateCategoryRequest(name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CategoryResponse.class)
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
