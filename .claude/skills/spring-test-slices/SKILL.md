---
name: spring-test-slices
description: Use when writing or reviewing tests in this Spring Boot 4 project. Triggers on requests to "add a test", "test the controller", "test the repository", "test the service", or any time `@SpringBootTest` is being reached for. Helps choose the right test slice and avoid loading the whole context unnecessarily.
---

# Test slices

Spring Boot **test slices** load only the beans needed for one layer. They start in milliseconds; `@SpringBootTest` starts in seconds. Default to slices.

## Decision table

| What you're testing | Annotation | Loads |
|---|---|---|
| Pure business logic in a service | none — plain JUnit + Mockito | nothing |
| `@RestController` request/response, validation, advice | `@WebMvcTest(MyController.class)` | MVC infra + that controller, mocked services |
| Repository / SQL / migrations | `@DataJdbcTest` | Datasource + Flyway + repositories |
| Full app wiring smoke test | `@SpringBootTest` | everything — keep to one or two |
| JSON serialization of a DTO | `@JsonTest` | Jackson only |

## Service layer — no Spring at all

```java
class ExpenseServiceTest {
    private final ExpenseRepository repo = mock();
    private final ExpenseService service = new ExpenseService(repo);

    @Test
    void create_persists_and_returns_with_id() { /* … */ }
}
```

Constructor injection makes this trivial. If you can't construct a service without Spring, the design is wrong, not the test.

## Controller — `@WebMvcTest`

```java
@WebMvcTest(ExpenseController.class)
class ExpenseControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ExpenseService service;  // Spring Boot 3.4+; replaces deprecated @MockBean

    @Test
    void create_returns_201() throws Exception {
        when(service.create(any())).thenReturn(/* … */);
        mvc.perform(post("/api/expenses")
                .contentType(APPLICATION_JSON)
                .content("""
                    {"description":"coffee","amount":3.50,"currency":"USD","incurredAt":"2026-05-09T08:00:00Z"}
                """))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.id").exists());
    }
}
```

- `@WebMvcTest` does **not** load services or repositories. Mock service collaborators with `@MockitoBean`.
- Use `MockMvc` for assertions; don't pull in `RestAssured` or `WebClient` here.
- Cover: happy path, validation failures (`400`), not-found (`404`) via the advice, and any custom error mapping.

## Repository / migrations — `@DataJdbcTest` + Testcontainers

```java
@DataJdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = NONE)  // crucial: don't replace with H2
class ExpenseRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired ExpenseRepository repo;

    @Test
    void saves_and_loads() { /* … */ }
}
```

- **Don't use H2.** Migrations are written in Postgres SQL; H2's dialect quirks (no `BIGSERIAL`, different array/JSON syntax) defeat the purpose of SQL-first migrations.
- `@AutoConfigureTestDatabase(replace = NONE)` prevents Spring from swapping in an in-memory DB.
- Flyway runs on context startup, so the test DB matches production schema.
- Reuse one container across the test class (static field) — startup is the slow part.

## `@SpringBootTest` — when it's actually warranted

- End-to-end smoke tests covering the whole wiring (the existing `contextLoads()` is the canonical example).
- Tests crossing multiple slices (controller + repo + real Flyway), where slicing would lose fidelity.
- Keep the count low. Each one is several seconds of startup per run.

Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` when you need a real HTTP round-trip.

## Don't

- `@SpringBootTest` + `@MockitoBean`-out-half-the-context to test a controller. Use `@WebMvcTest`.
- Sleep/poll in tests. Use `Awaitility` if you must wait on async state.
- `@Transactional` on tests as a "rollback" hack to clean up. Prefer Testcontainers per-class — your tests should reflect real commit semantics, especially with Spring Data JDBC where there's no flush/clear to confuse.
- Sharing a Postgres container across unrelated test classes via static + start-once unless you also clean state between them. Drift is hard to debug.
