# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Early-stage Spring Boot 4.0.6 project. Only `ExpensesApplication` exists on the Java side — no controllers, entities, services, or repositories yet. Schema is managed by Flyway with one migration so far ([V1__create_expense_table.sql](src/main/resources/db/migration/V1__create_expense_table.sql) — `expense` table). Treat new Java code as net-new design.

## Project skills

Project-level Claude skills live under [.claude/skills/](.claude/skills/). Consult them before making changes in their domains:

- **create-migration** — Flyway conventions, file naming, immutability rule.
- **spring-data-jdbc-patterns** — aggregate roots, why this is *not* JPA, mapping rules.
- **spring-rest-controller** — DTOs, validation, `@RestControllerAdvice` + `ProblemDetail`.
- **spring-test-slices** — choosing between `@WebMvcTest`, `@DataJdbcTest` (with Testcontainers, no H2), and `@SpringBootTest`.
- **spring-configuration-properties** — `@ConfigurationProperties` records over `@Value`.
- **exception-conventions** — custom exceptions live in `com.tincho.expenses.exceptions`; every one is wired through `ApiExceptionHandler`.
- **postgres-enum-mapping** — Postgres native ENUM + Spring Data JDBC requires `?stringtype=unspecified` in the JDBC URL; Java enum constants must match SQL literals exactly.
- **readme-api-docs** — every change to the HTTP API surface (endpoints, DTOs, validation, roles, error statuses) must update the `## API Reference` section of [README.md](README.md) in the same change.
- **tanstack-query** — `useQuery` for GET/data fetching, `useMutation` for writes, cache invalidation patterns, query key conventions. Use whenever adding data fetching or form submission in the React frontend.
- **formik** — `useFormik` + Yup schema validation + MUI TextField wiring. Use whenever adding any form to the React frontend.

## Stack

- **Java 26** (Gradle toolchain — auto-provisioned, no need to install manually)
- **Spring Boot 4.0.6** with `spring-boot-starter-webmvc` (servlet stack) and `spring-boot-starter-data-jdbc` (Spring Data **JDBC**, not JPA — aggregate-root oriented, no lazy loading, no entity manager)
- **PostgreSQL** at runtime; datasource URL/user/pass come from `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` env vars, defaulting to `localhost:5432/expenses` + `postgres`/`postgres` for local dev
- **Flyway** for schema — migrations in [src/main/resources/db/migration/](src/main/resources/db/migration/), run automatically on context startup. `spring.sql.init.mode=never` is set to keep Flyway as the single source of schema truth
- **Lombok** with annotation processor (both main and test)
- **Spring Boot DevTools** (development-only auto-restart)

## Commands

```bash
docker compose up -d                                 # start local Postgres (reads .env)
docker compose down                                  # stop, keep volume
docker compose down -v                               # stop and wipe data volume
./gradlew bootRun                                    # run the app (needs Postgres up)
./gradlew build                                      # full build incl. tests
./gradlew test                                       # run all tests
./gradlew test --tests ExpensesApplicationTests      # run a single test class
./gradlew test --tests '*ClassName.methodName'       # run a single test method
./gradlew bootJar                                    # produce executable jar in build/libs
```

There is no separate lint/format task configured.

## Local database

[docker-compose.yaml](docker-compose.yaml) runs Postgres 16 with credentials from `.env` (gitignored; mirror in [.env.example](.env.example)). The defaults in `.env` match the defaults in [application.yaml](src/main/resources/application.yaml) (`postgres`/`postgres`, db `expenses`), so `docker compose up -d && ./gradlew bootRun` works out of the box. Production deploys will use different credentials injected via `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` env vars — do **not** put production credentials in `.env`.

## Architecture notes for future work

- **Spring Data JDBC, not JPA** — see the `spring-data-jdbc-patterns` skill for the full picture. Short version: aggregate-root repositories only, no lazy loading, references across aggregates use `AggregateReference`.
- **Servlet web stack** (`spring-boot-starter-webmvc`), not WebFlux — controllers are blocking `@RestController` / `@Controller`.
- **Test slices**: prefer `@DataJdbcTest` (with Testcontainers Postgres — **not H2**) and `@WebMvcTest` over full `@SpringBootTest`. See the `spring-test-slices` skill. The existing `ExpensesApplicationTests.contextLoads()` will need a reachable Postgres (or Testcontainers wiring) to pass now that Flyway is enabled.
- **Schema is SQL, not derived** — never let any tool generate DDL. All schema goes through Flyway migrations; see the `create-migration` skill.
- **Base package** is `com.tincho.expenses` — keep new packages under it so component scanning picks them up automatically.
