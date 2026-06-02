# Backend pagination skill

## When to use this skill

Use whenever adding server-side pagination to a list endpoint in this Spring Boot + Spring Data JDBC project.

Trigger phrases: "add pagination", "paginate the endpoint", "add page/size params", "return Page", "list endpoint returns too much data".

---

## How pagination works in this project

Spring Data JDBC does not auto-apply `Pageable` to custom `@Query` methods the way JPA does. The pattern here is:

1. **Repository** — two custom queries per entity: a `LIMIT/OFFSET` query and a `COUNT` query, plus extend `ListPagingAndSortingRepository` for the admin `findAll(Pageable)`.
2. **Service** — a `listForTenant(Long tenantId, Pageable pageable)` overload that assembles a `PageImpl<T>`, and a `listAll(Pageable pageable)` that delegates to `repository.findAll(pageable)`.
3. **Controller** — `list()` accepts `@RequestParam int page` (default 0) and `@RequestParam int size` (default 20), builds a `PageRequest`, and returns `PageResponse<T>`.
4. **Response DTO** — `PageResponse<T>` (already defined in `com.tincho.expenses.controllers.dto`) wraps `Page<T>` and serialises only the fields the frontend needs.

---

## Repository

Extend both `ListCrudRepository` and `ListPagingAndSortingRepository`. Keep the original unfiltered `findByTenantId` (used by service unit tests). Add `findByTenantIdPaged` + `countByTenantId`:

```java
public interface ExpenseRepository
        extends ListCrudRepository<Expense, Long>,
                ListPagingAndSortingRepository<Expense, Long> {

    // existing — kept for service unit tests
    @Query("SELECT * FROM expense WHERE tenant_id = :tenantId ORDER BY incurred_at DESC, id DESC")
    List<Expense> findByTenantId(@Param("tenantId") Long tenantId);

    // paginated tenant query
    @Query("SELECT * FROM expense WHERE tenant_id = :tenantId ORDER BY incurred_at DESC, id DESC LIMIT :size OFFSET :offset")
    List<Expense> findByTenantIdPaged(@Param("tenantId") Long tenantId,
                                      @Param("size") int size,
                                      @Param("offset") long offset);

    @Query("SELECT COUNT(*) FROM expense WHERE tenant_id = :tenantId")
    long countByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT * FROM expense WHERE id = :id AND tenant_id = :tenantId")
    Optional<Expense> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
```

`ListPagingAndSortingRepository` inherits `Page<T> findAll(Pageable)` from `PagingAndSortingRepository` — no extra query needed for admin pagination.

---

## Service

Keep the existing `List<T>` overloads (unit tests depend on them). Add `Page<T>` overloads:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

// tenant-scoped — manual count + LIMIT/OFFSET
public Page<Expense> listForTenant(Long tenantId, Pageable pageable) {
    List<Expense> content = repository.findByTenantIdPaged(
            tenantId, pageable.getPageSize(), pageable.getOffset());
    long total = repository.countByTenantId(tenantId);
    return new PageImpl<>(content, pageable, total);
}

// admin — framework handles pagination
public Page<Expense> listAll(Pageable pageable) {
    return repository.findAll(pageable);
}
```

---

## Controller

Add `@Validated` to the class. Add `page` and `size` request params with constraints. Return `PageResponse<T>`:

```java
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Validated
class ExpenseController {

    @GetMapping
    PageResponse<ExpenseResponse> list(
            @AuthenticationPrincipal TenantUserDetails principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<Expense> expenses = ADMIN.equals(principal.getRole())
                ? service.listAll(PageRequest.of(page, size))
                : service.listForTenant(principal.getId(), PageRequest.of(page, size));
        return PageResponse.from(expenses.map(ExpenseResponse::from));
    }
}
```

---

## Response DTO

`PageResponse<T>` is already defined in `com.tincho.expenses.controllers.dto`. Do **not** return Spring's `PageImpl` directly from controllers — Jackson would serialise internal Spring fields. Always wrap via `PageResponse.from(page)`:

```java
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,   // 0-indexed
        int size
) {
    public static <T> PageResponse<T> from(Page<T> page) { ... }
}
```

---

## Updating tests

### Controller unit tests (`@ExtendWith(MockitoExtension.class)`)

- Stub the paginated overload: `given(service.listForTenant(tenantId, PageRequest.of(0, 20))).willReturn(new PageImpl<>(List.of(...)))`
- Call with explicit args: `controller.list(principal, 0, 20)`
- Assert on `.content()`: `assertThat(response.content()).containsExactly(...)`
- For the "never" verify: `verify(service, never()).listAll(PageRequest.of(0, 20))`; use `any()` for the Pageable arg in the other direction

```java
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

given(service.listForTenant(7L, PageRequest.of(0, 20)))
        .willReturn(new PageImpl<>(List.of(a, b)));

PageResponse<ExpenseResponse> response = controller.list(principal, 0, 20);

assertThat(response.content()).containsExactly(ExpenseResponse.from(a), ExpenseResponse.from(b));
```

### Integration tests (`@SpringBootTest`)

Replace `ParameterizedTypeReference<List<XxxResponse>>` with `ParameterizedTypeReference<PageResponse<XxxResponse>>` and access items via `.content()`:

```java
.expectBody(new ParameterizedTypeReference<PageResponse<ExpenseResponse>>() {})
.value(page -> assertThat(page.content())
        .extracting(ExpenseResponse::description)
        .containsExactlyInAnyOrder("A-Lunch", "A-Dinner"));
```

Remove the `import java.util.List` from integration test files if it is no longer used.

---

## What NOT to do

- Do not use `@Query` with a `Pageable` parameter in Spring Data JDBC — it does not automatically translate to `LIMIT/OFFSET`. Use explicit named params.
- Do not return `Page<T>` or `PageImpl<T>` directly from controllers — always wrap in `PageResponse<T>`.
- Do not remove the existing `List<T>` service methods (`listForTenant(Long)`, `listAll()`) — service unit tests stub those directly and will break if removed.
- Do not add `LIMIT/OFFSET` to the original `findByTenantId` query — keep it separate so unit tests don't need to change.
