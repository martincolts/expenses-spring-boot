---
name: fix-tests-after-backend-change
description: Use whenever a backend model, DTO, or service signature changes. Triggers on adding/removing fields to a record or class, changing constructor parameters, renaming methods, or any modification that would cause test compilation errors. Covers the four test layers this project ships per feature.
---

# Fixing tests after a backend change

Every time you change a Java record, DTO, or service method signature, you must update **all** test files that reference it before the build can succeed. Follow this checklist every time.

## Step 1 — find every affected test file

```bash
grep -rl "ClassName\|methodName" src/test/
```

Run this for every changed type or method. Common files to check per feature:

| Layer | File pattern |
|---|---|
| Controller unit | `src/test/…/controllers/<Feature>ControllerTest.java` |
| Service unit | `src/test/…/services/<Feature>ServiceTest.java` |
| Repository slice | `src/test/…/repositories/<Feature>RepositoryTest.java` |
| Integration | `src/test/…/<Feature>IntegrationTest.java` |

Also grep other features' test files — they may use a shared helper (e.g. `ExpenseRepositoryTest` creates a `Category`).

## Step 2 — fix record/DTO constructors

When a record gains or loses fields, every `new TheRecord(…)` in tests must be updated.

**Pattern — adding nullable fields:**  
Pass `null` for new optional fields that tests don't care about:
```java
// Before
new Category(id, tenant, name, createdAt)
// After (iconId, colorId added)
new Category(id, tenant, name, null, null, createdAt)
```

**Pattern — adding fields with defaults to DTOs:**  
```java
// Before
new CreateCategoryRequest("Food")
// After
new CreateCategoryRequest("Food", null, null)
```

Use `replace_all: true` only when the literal string is identical across all occurrences. When the argument differs (e.g. `"Food"`, `"Travel"`, `"A-Food"`), fix each occurrence individually.

## Step 3 — fix service/mock stubs that reconstruct the model

Anonymous answer lambdas in Mockito stubs often reconstruct the entity — they need updating too:

```java
// Before
.willAnswer(inv -> {
    Category c = inv.getArgument(0);
    return new Category(42L, c.tenant(), c.name(), c.createdAt());
});
// After
.willAnswer(inv -> {
    Category c = inv.getArgument(0);
    return new Category(42L, c.tenant(), c.name(), c.iconId(), c.colorId(), c.createdAt());
});
```

## Step 4 — fix private helper methods in test classes

Each test class typically has a `private static Category category(…)` or similar factory helper at the bottom. Update these too:

```java
private static Category category(Long id, Long tenantId, String name) {
    return new Category(id, AggregateReference.<Tenant, Long>to(tenantId), name, null, null, OffsetDateTime.now());
}
```

## Step 5 — compile and run

```bash
# Fast check — compile only
./gradlew compileTestJava

# Run the affected test class(es)
./gradlew test --tests '*CategoryControllerTest' --tests '*CategoryServiceTest'

# Full suite when confident
./gradlew test
```

Fix compilation errors before running. The compiler error message always shows the required vs. found argument lists — use that to count how many `null`s to add.

## IDE diagnostics lag

The IDE language server may report stale diagnostics after an edit. Always trust the Gradle compiler output over IDE squiggles — run `./gradlew compileTestJava` to get the authoritative list of remaining errors.
