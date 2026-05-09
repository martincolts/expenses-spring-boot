# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Fresh Spring Boot 4.0.6 skeleton (generated, not yet built out). Only `ExpensesApplication` exists — no controllers, entities, repositories, services, or migrations have been added yet. Treat new files as net-new design, not edits to existing patterns.

## Stack

- **Java 26** (Gradle toolchain — auto-provisioned, no need to install manually)
- **Spring Boot 4.0.6** with `spring-boot-starter-webmvc` (servlet stack) and `spring-boot-starter-data-jdbc` (Spring Data **JDBC**, not JPA — aggregate-root oriented, no lazy loading, no entity manager)
- **PostgreSQL** as the runtime DB driver — `application.yaml` has no datasource configured yet, so the app will not start until `spring.datasource.*` is set
- **Lombok** with annotation processor (both main and test)
- **Spring Boot DevTools** (development-only auto-restart)

## Commands

```bash
./gradlew bootRun                                    # run the app
./gradlew build                                      # full build incl. tests
./gradlew test                                       # run all tests
./gradlew test --tests ExpensesApplicationTests      # run a single test class
./gradlew test --tests '*ClassName.methodName'       # run a single test method
./gradlew bootJar                                    # produce executable jar in build/libs
```

There is no separate lint/format task configured.

## Architecture notes for future work

- **Spring Data JDBC, not JPA** — when adding persistence, use `@Table`, `@Id`, `CrudRepository`/`ListCrudRepository` from `org.springframework.data.repository`. Don't reach for `@Entity`, `EntityManager`, or JPA-specific annotations; they aren't on the classpath. Model relationships as aggregate roots referencing children, not bidirectional JPA associations.
- **Servlet web stack** (`spring-boot-starter-webmvc`), not WebFlux — controllers are blocking `@RestController` / `@Controller`.
- **Test slices**: the project pulls in `spring-boot-starter-data-jdbc-test` and `spring-boot-starter-webmvc-test`, so prefer `@DataJdbcTest` and `@WebMvcTest` over full `@SpringBootTest` for fast, focused tests. The existing `ExpensesApplicationTests.contextLoads()` is the only full-context test and will require a working datasource (or test slice replacement) once DB config is added.
- **Base package** is `com.tincho.expenses` — keep new packages under it so component scanning picks them up automatically.
