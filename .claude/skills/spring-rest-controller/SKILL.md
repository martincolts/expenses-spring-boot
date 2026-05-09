---
name: spring-rest-controller
description: Use when adding or modifying HTTP endpoints in this Spring Boot 4 project. Triggers on requests to "add an endpoint", "expose an API", "create a controller", "return JSON", or anything involving `@RestController`, `@RequestMapping`, request/response DTOs, validation, or error handling.
---

# REST controller conventions

## Layering

`Controller → Service → Repository`. Controllers translate HTTP ↔ DTO and delegate; they don't contain business logic and they don't talk to repositories directly.

## DTOs, not entities, on the wire

- Request and response bodies are **records** in a `dto` (or `api`) package — never the persistence entity.
- Reasons: decouples API shape from schema, prevents accidental field exposure (e.g., internal IDs, audit columns), avoids serialization surprises with Spring Data JDBC aggregates.
- Map at the controller boundary. A `Mapper` class or static factory methods on the DTO is fine; don't pull in MapStruct unless mapping volume justifies it.

```java
public record CreateExpenseRequest(
    @NotBlank String description,
    @NotNull @Positive BigDecimal amount,
    @NotBlank @Size(min = 3, max = 3) String currency,
    String category,
    @NotNull OffsetDateTime incurredAt
) {}

public record ExpenseResponse(Long id, String description, BigDecimal amount, /* … */) {
    public static ExpenseResponse from(Expense e) { /* … */ }
}
```

## Controller shape

```java
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
class ExpenseController {

    private final ExpenseService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ExpenseResponse create(@Valid @RequestBody CreateExpenseRequest req) {
        return ExpenseResponse.from(service.create(req));
    }

    @GetMapping("/{id}")
    ExpenseResponse get(@PathVariable Long id) {
        return ExpenseResponse.from(service.get(id));  // service throws if missing
    }
}
```

- Class is **package-private** — Spring doesn't need it public, and keeping it package-private prevents accidental cross-package coupling.
- Constructor injection via Lombok's `@RequiredArgsConstructor` on `final` fields. No field injection (`@Autowired` on fields). No setter injection.
- Return DTOs directly (Spring serializes them) rather than `ResponseEntity<DTO>`, **unless** you need to set headers, vary status, or return `204 No Content`.
- Use `@ResponseStatus` for fixed non-200 success codes (e.g., `201` on create, `204` on delete).
- Path: prefix with `/api/` to leave room for non-API paths (actuator, static, etc.). Use plural nouns (`/expenses`), not verbs.

## Validation

- `@Valid` on the request body parameter; constraints (`@NotBlank`, `@NotNull`, `@Positive`, `@Size`, `@Pattern`) on the record components.
- Cross-field validation: write a custom `@Constraint` or validate in the service — don't smear it across the controller.

## Exception handling

Centralize via `@RestControllerAdvice`, never per-controller try/catch:

```java
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ProblemDetail notFound(EntityNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail validation(MethodArgumentNotValidException ex) { /* … */ }
}
```

- Prefer `ProblemDetail` (RFC 7807) over ad-hoc error objects — Spring 6+/Boot 3+ ships first-class support.
- Throw domain exceptions from services. Let the advice translate them to status codes. Controllers stay clean.

## Things to avoid

- Returning entities directly (leaks schema, breaks on lazy/aggregate loading).
- `ResponseEntity<?>` everywhere. It's noise when status is fixed and headers are default.
- `@Autowired` on fields, or constructors with multiple constructors. One constructor → Spring auto-wires it, no annotation needed.
- Catching `Exception` in controllers. Let it bubble to the advice.
- Routing logic with `if (request.type().equals("X"))` — split into separate endpoints or use polymorphic request types with `@JsonTypeInfo`.
