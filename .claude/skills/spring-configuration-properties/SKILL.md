---
name: spring-configuration-properties
description: Use when adding configuration knobs, reading values from application.yaml, or seeing `@Value` in code under review. Triggers on requests to "make X configurable", "read this from config", "add a property for Y", or any introduction of environment-specific values into the codebase.
---

# Configuration properties

Prefer **`@ConfigurationProperties` records** over `@Value`. The difference is real — type safety, validation, IDE completion, refactor safety, and one place to see everything a feature reads from config.

## The shape

```java
@ConfigurationProperties("expenses.import")
@Validated
public record ImportProperties(
    @NotNull Duration timeout,
    @Positive int batchSize,
    @NotBlank String defaultCurrency,
    Retry retry
) {
    public record Retry(@PositiveOrZero int maxAttempts, @NotNull Duration backoff) {}
}
```

In `application.yaml`:

```yaml
expenses:
  import:
    timeout: 30s
    batch-size: 500
    default-currency: USD
    retry:
      max-attempts: 3
      backoff: 2s
```

Enable scanning once at the application class:

```java
@SpringBootApplication
@ConfigurationPropertiesScan  // picks up @ConfigurationProperties records anywhere in the package tree
public class ExpensesApplication { /* … */ }
```

Then inject like any bean:

```java
@Service
@RequiredArgsConstructor
class ImportService {
    private final ImportProperties props;
    // use props.batchSize(), props.retry().maxAttempts(), …
}
```

## Why not `@Value`?

- `@Value("${expenses.import.timeout}")` resolves at field-injection time as a string — no compile-time check, no validation, no relaxed binding, no IDE help.
- Scattering `@Value` across classes hides the full config surface of a feature. A record concentrates it.
- Type conversion of complex types (`Duration`, `DataSize`, lists, maps, nested records) is ergonomic with `@ConfigurationProperties`, awkward with `@Value`.

Acceptable uses of `@Value`: one-off Spring built-ins (`@Value("${server.port}")`), or trivial single strings in test config. Anything else → record.

## Property naming

- YAML uses `kebab-case` (`batch-size`); Java uses `camelCase` (`batchSize`). Spring Boot's relaxed binding handles the bridge.
- Prefix with the project/feature: `expenses.import.*`, not `import.*`. Keeps Spring's own namespaces (`spring.*`, `management.*`, `server.*`) clean.

## Validation

- Annotate the class with `@Validated` and put JSR-380 constraints (`@NotNull`, `@Positive`, `@NotBlank`, `@Pattern`, `@Size`) on record components.
- Validation runs on context start — misconfiguration fails fast with a clear error rather than blowing up at runtime on the first call.

## Profiles

- Environment differences live in `application-<profile>.yaml` (e.g., `application-local.yaml`, `application-prod.yaml`), activated via `SPRING_PROFILES_ACTIVE`.
- Don't branch on `@Profile` inside `@ConfigurationProperties` — it's just data. Branch in `@Configuration` classes that build conditional beans.

## Secrets

- Never put real secrets in `application.yaml`. Use env-var substitution: `password: ${DB_PASSWORD:postgres}` (default for local dev only).
- For production, prefer the platform's secret manager (env vars injected by k8s/ECS/Vault). Don't roll your own decryption.

## Migrating from `@Value`

When you find `@Value` in this codebase, treat it as a refactor signal. Move all related properties into one record, switch consumers to inject the record, delete the `@Value` annotations. Keep the YAML keys identical to avoid an ops change.
