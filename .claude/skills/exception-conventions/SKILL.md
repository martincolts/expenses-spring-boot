---
name: exception-conventions
description: Use when adding, moving, or renaming a domain exception in this Spring Boot project. Triggers on requests to "add a NotFoundException", "throw a domain exception", "create an XAlreadyExistsException", or anywhere a service is about to `throw new` a custom RuntimeException. Critical because this project keeps all custom exceptions in a dedicated `exceptions` package and wires every one through `ApiExceptionHandler`.
---

# Exception conventions

All custom domain exceptions in this project live in a single package and are mapped to HTTP responses in one place. Don't put exception classes next to the services that throw them, and don't handle them per-controller.

## Package layout

- **Location:** `com.tincho.expenses.exceptions` — `src/main/java/com/tincho/expenses/exceptions/`.
- **One class per file**, named `<Aggregate><Condition>Exception` (e.g. `TenantNotFoundException`, `CategoryNameAlreadyExistsException`).
- **Extend `RuntimeException`.** Checked exceptions are not used here — services throw unchecked and the `@RestControllerAdvice` translates them.
- **No package-info, no marker interfaces, no shared base class** unless multiple exceptions truly share behavior. Flat is fine.

```java
package com.tincho.expenses.exceptions;

public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(Long id) {
        super("Tenant " + id + " not found");
    }
}
```

The constructor encodes the message format — callers pass only the identifying value (`new TenantNotFoundException(id)`), never a pre-formatted string. This keeps wording consistent and makes the exception easy to grep for.

## Mapping to HTTP

Every exception in `exceptions/` must have a corresponding `@ExceptionHandler` in [`ApiExceptionHandler`](../../../src/main/java/com/tincho/expenses/controllers/ApiExceptionHandler.java). If you add an exception without wiring it, the client gets a generic 500.

```java
@ExceptionHandler(TenantNotFoundException.class)
ProblemDetail notFound(TenantNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
}
```

Conventional status codes:

| Condition | Status | Example exception |
|---|---|---|
| Resource missing | `404 NOT_FOUND` | `TenantNotFoundException`, `CategoryNotFoundException` |
| Uniqueness violation | `409 CONFLICT` | `TenantEmailAlreadyExistsException`, `CategoryNameAlreadyExistsException` |
| Business-rule failure on valid input | `422 UNPROCESSABLE_ENTITY` or `400 BAD_REQUEST` | `InvalidCurrentPasswordException` |
| Authentication failure | `401 UNAUTHORIZED` | (reuse Spring's `BadCredentialsException`) |
| Authorization failure | `403 FORBIDDEN` | (reuse Spring's `AccessDeniedException`) |

If a new exception doesn't fit one of these slots, pause and reconsider the design before inventing a new status mapping.

## When to add an exception vs. reuse one

- **Reuse Spring exceptions** for auth (`BadCredentialsException`, `AccessDeniedException`) and validation (`MethodArgumentNotValidException`). These are already wired in `ApiExceptionHandler`.
- **Add a project exception** when the service layer needs to signal a domain failure that the controller advice should turn into a specific status — typically not-found and conflict cases per aggregate.
- **Don't add per-method exceptions.** One not-found per aggregate, one conflict per uniqueness rule, that's usually enough.

## Checklist when adding a new exception

1. Create `src/main/java/com/tincho/expenses/exceptions/<Name>Exception.java` extending `RuntimeException`.
2. Constructor accepts identifying values, not formatted strings. Build the message inside.
3. Add an `@ExceptionHandler` method to `ApiExceptionHandler` mapping it to the right status.
4. Throw it from the service layer (not from the controller, repository, or filter).
5. Cover it in the service unit test (`assertThatThrownBy(...).isInstanceOf(...)` plus `verify(repo, never()).save(any())`) and the integration test (assert HTTP status + body contains a recognizable detail).
6. Document the resulting status in the relevant section of `README.md`'s API Reference.

## Common mistakes to avoid

- Putting exceptions in the `services/` package alongside the service that throws them. They moved out deliberately — controllers, filters, and other services also throw and catch them, so they don't belong to one layer.
- Catching a domain exception in a controller to translate it manually. Let it bubble; the advice handles it.
- Adding an exception without a handler. Forgotten ⇒ 500 in production. Always update `ApiExceptionHandler` in the same change.
- Reaching for a generic `EntityNotFoundException` for every aggregate. Per-aggregate types make stack traces and messages clearer and keep handler logic explicit.
