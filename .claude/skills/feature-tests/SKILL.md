---
name: feature-tests
description: Use when adding tests for a new feature/aggregate in this Spring Boot 4 project. Triggers on "test the tenant flow", "add tests for X", or whenever a new entity + service + controller is added and needs full test coverage. This is the *template* guide — concrete copy-pasteable patterns for the four test files this project ships per feature. Pairs with `spring-test-slices`, which is the *decision* guide.
---

# Feature tests

Every feature in this codebase ships with **four test files**, one per layer plus one full-stack integration test. The tenant feature is the canonical reference — read those files alongside this skill if you need to see them in context:

- [TenantRepositoryTest](../../../src/test/java/com/tincho/expenses/repositories/TenantRepositoryTest.java) — `@DataJdbcTest` + Testcontainers Postgres
- [TenantServiceTest](../../../src/test/java/com/tincho/expenses/services/TenantServiceTest.java) — plain JUnit + Mockito, no Spring
- [TenantControllerTest](../../../src/test/java/com/tincho/expenses/controllers/TenantControllerTest.java) — plain JUnit + Mockito, no Spring
- [TenantIntegrationTest](../../../src/test/java/com/tincho/expenses/TenantIntegrationTest.java) — `@SpringBootTest(RANDOM_PORT)` + `RestTestClient` + Testcontainers

## Why this layout (and not just `@WebMvcTest`)

The project deliberately tests controllers with plain Mockito (no `@WebMvcTest`) and reaches for a single full-stack integration test for HTTP-level coverage. This trades some controller-slice fidelity for:

- **Faster controller unit tests** (no Spring context at all → milliseconds).
- **Higher-fidelity HTTP coverage** in one place — the integration test exercises validation, `@RestControllerAdvice`, JSON serialization, and Postgres in a single round-trip, against the same beans production uses.

If you ever need to test something that *only* lives in the MVC layer (filters, argument resolvers, custom converters), do reach for `@WebMvcTest` — see `spring-test-slices`. For ordinary controller logic, follow the patterns below.

## 1. Repository test — `@DataJdbcTest` + Testcontainers

Boots datasource + Flyway + repositories. Real Postgres, never H2 (Flyway SQL is Postgres-specific).

```java
@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class FooRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    FooRepository repository;

    @Test
    void save_assignsId() { /* … */ }
}
```

What to cover:
- `save` assigns the id and round-trips through `findById`.
- Each finder/`existsBy` method — present and absent cases.
- DB-level constraints (unique, FK) — assert `DataAccessException` is thrown on violation. This catches migration drift between the schema and the entity.

Test isolation: use unique values per test (e.g. unique emails). The container is shared across the class, so don't rely on an empty table.

## 2. Service unit test — plain JUnit + Mockito

No Spring context. Constructor injection makes the service trivially constructible.

```java
@ExtendWith(MockitoExtension.class)
class FooServiceTest {

    @Mock FooRepository repository;
    @Mock PasswordEncoder passwordEncoder; // example collaborator

    @InjectMocks FooService service;

    @Test
    void create_persists_withEncodedPassword() {
        var req = new CreateFooRequest(/* … */);
        given(repository.existsByEmail("x@y")).willReturn(false);
        given(passwordEncoder.encode("plain")).willReturn("hash");
        given(repository.save(any(Foo.class)))
                .willAnswer(inv -> withId(1L, inv.getArgument(0)));

        Foo created = service.create(req);

        ArgumentCaptor<Foo> captor = ArgumentCaptor.forClass(Foo.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().passwordHash()).isEqualTo("hash");
        assertThat(created.id()).isEqualTo(1L);
    }
}
```

What to cover:
- Happy path: capture the argument passed to the repo and assert each field, including derived values (encoded password, computed timestamp). Then assert the returned object is what the repo gave back.
- Each business-rule branch (duplicate email, missing entity, etc.) → assert the right exception type and that side-effects (`repository.save`, `passwordEncoder.encode`) are **not** invoked. Use `verify(x, never()).method(any())`.
- Pure read-throughs (`list`, `get`) — short tests that confirm delegation.

## 3. Controller unit test — plain JUnit + Mockito

The controller is a thin mapping layer. Test it as a plain class with the service mocked.

```java
@ExtendWith(MockitoExtension.class)
class FooControllerTest {

    @Mock FooService service;
    @InjectMocks FooController controller;

    @Test
    void create_delegatesToService_andReturnsResponse() {
        var req = new CreateFooRequest(/* … */);
        var saved = new Foo(1L, /* … */);
        given(service.create(req)).willReturn(saved);

        FooResponse response = controller.create(req);

        verify(service).create(req);
        assertThat(response).isEqualTo(FooResponse.from(saved));
    }
}
```

What to cover:
- Each endpoint: delegation to the service and DTO mapping.
- Exception propagation (`willThrow` on the mock → `assertThatThrownBy(...).isSameAs(boom)`). The actual HTTP status mapping lives in `ApiExceptionHandler` and is exercised by the integration test.
- Don't assert HTTP status, JSON shape, or validation here — that belongs in the integration test.

## 4. Integration test — `@SpringBootTest` + `RestTestClient` + Testcontainers

Real server, real Postgres, real Flyway, real exception handler, real Jackson. One per feature.

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureRestTestClient
@Testcontainers
class FooIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired RestTestClient client;
    @Autowired JdbcTemplate jdbc;
    @Autowired FooRepository repository;

    @BeforeEach
    void cleanup() {
        jdbc.execute("TRUNCATE TABLE foo RESTART IDENTITY CASCADE");
    }

    @Test
    void create_returns201() {
        client.post().uri("/api/foos")
                .body(new CreateFooRequest(/* … */))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FooResponse.class)
                .value(r -> assertThat(r.id()).isNotNull());
    }
}
```

What to cover (the integration test is the primary place for HTTP-level assertions):
- Happy path for every endpoint, asserting status + body.
- Validation failures → `400` (e.g. blank field, invalid email, too short). Also assert state was not mutated (`repository.findByEmail(...)` is empty).
- Domain errors → mapped status (`409` for duplicate, `404` for missing). Assert the response body contains a recognizable detail.
- Side-effects worth verifying through the repo: password is hashed and not stored plaintext, fields preserved on update, row removed on delete.

### Spring Boot 4 RestTestClient gotchas

- `RestTestClient` lives in **`spring-boot-resttestclient`** — a separate artifact, not pulled in by `spring-boot-starter-webmvc-test`. The dependency is already added to [build.gradle](../../../build.gradle); leave it there.
- The bean is registered by `@AutoConfigureRestTestClient`. Without that annotation, `@Autowired RestTestClient` will fail to inject.
- API differences vs. `WebTestClient`/old `TestRestTemplate`:
  - Use `.body(obj)` (not `.bodyValue(obj)`).
  - There is **no** `expectBodyList(Class)`. For a list response use `expectBody(new ParameterizedTypeReference<List<Foo>>() {}).value(list -> …)`.
  - `expectBody(Class).value(consumer)` is the idiomatic way to chain AssertJ on the deserialized body.

### Test isolation

The integration test class shares a single Postgres container; `TRUNCATE … RESTART IDENTITY CASCADE` in `@BeforeEach` keeps tests independent and resets the id sequence so `9999` is reliably "missing." Don't use `@Transactional`-rollback for cleanup — Spring Data JDBC has no flush/clear and you want to test real commit semantics.

## Conventions across all four files

- **AssertJ** for assertions (`assertThat(...)`), never JUnit's `assertEquals` / `assertTrue`.
- **Method naming**: `methodUnderTest_outcome[_condition]`, e.g. `create_returns201_andPersistsEncodedPassword`, `update_throws_whenTenantMissing`. The condition clause is optional but encouraged when there are multiple branches.
- **One assertion concept per test.** Multiple `assertThat` calls are fine when they describe the same outcome (e.g. saved entity has correct fields).
- **Don't use** `@MockitoBean` / `@MockBean` to mock half the application out of a `@SpringBootTest` to "make it like a slice" — write the slice/unit test instead.
- **Don't** sleep or poll. If you ever need to wait on async state, use `Awaitility` (not currently a dependency — add deliberately).

## Adding the test files when scaffolding a feature

When adding `Foo`, create all four test files in the same change:

| File | Path |
|---|---|
| Repository test | `src/test/java/com/tincho/expenses/repositories/FooRepositoryTest.java` |
| Service unit test | `src/test/java/com/tincho/expenses/services/FooServiceTest.java` |
| Controller unit test | `src/test/java/com/tincho/expenses/controllers/FooControllerTest.java` |
| Integration test | `src/test/java/com/tincho/expenses/FooIntegrationTest.java` |

The integration test sits at the package root (alongside `ExpensesApplicationTests`) because it's a cross-cutting test, not a layer test.
