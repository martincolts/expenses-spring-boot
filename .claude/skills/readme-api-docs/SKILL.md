---
name: readme-api-docs
description: Use whenever the HTTP API surface of this project changes. Triggers when adding, removing, or modifying any `@RestController` endpoint, request/response DTO field, validation constraint, required role, or error mapping — i.e. anything a consumer of the API would observe. The [README.md](../../../README.md) "API Reference" section is the contract and must be updated in the **same** change.
---

# README API documentation

The [README.md](../../../README.md) at the repo root contains the canonical API Reference. It is the only place this project documents endpoints — there is no OpenAPI/Swagger output and no separate docs site. If the code and the README disagree, the README is wrong by definition and must be fixed in the same commit/PR as the code change.

## When this skill triggers

Anything that changes what a caller sees on the wire:

- Adding/removing/renaming an endpoint (route, HTTP verb, path variable).
- Adding/removing a field on a request or response DTO, or changing its type.
- Tightening or loosening a validation constraint (`@NotNull`, `@Size`, `@Positive`, regex, etc.).
- Changing the role required by `@PreAuthorize`, or the tenant-vs-admin branching in a controller.
- Adding a new domain exception wired through `ApiExceptionHandler` (new status code surfaced by an endpoint).
- Renaming a JSON property (record component, `@JsonProperty`).

You do **not** need to update the README for: internal refactors, repository/service signature changes, log messages, or migration files that don't alter the observed API.

## What to update

The README organizes endpoints by aggregate under `## API Reference`. For each affected endpoint, keep these in sync:

1. **HTTP verb + path** (the fenced block at the top of the subsection).
2. **One-paragraph description**, including any tenant-scoping or admin behavior.
3. **Required role** line.
4. **Request body** JSON example and the **field table** (`Field` / `Type` / `Constraints`). Constraints must match the validation annotations on the DTO record exactly — if the code says `@Size(max = 50)`, the table says `max 50 chars`.
5. **Response — `<status>`** JSON example, matching the response DTO's record components and Jackson serialization.
6. **`curl` example** — keep it copy-pasteable, with `$TOKEN` for authenticated calls.
7. **Errors table** — one row per status code the endpoint can return. Match what `ApiExceptionHandler` actually maps.

When adding a brand-new aggregate, follow the existing structure (see the **Categories** or **Expenses** sections as templates): a short intro paragraph, then Create / List / Get / Update / Delete subsections in that order, each separated by `---`.

## Checklist before declaring an API change done

- [ ] DTO field changes are reflected in both the request/response JSON example **and** the field table.
- [ ] New error statuses introduced by a new `@ExceptionHandler` are listed in the **Errors** table of every endpoint that can throw them.
- [ ] Role/visibility rules (`@PreAuthorize`, admin bypass branches) match the prose under **Required role**.
- [ ] The `curl` example actually works against the documented body and path.
- [ ] If a new aggregate was added, it has its own `###` section and appears in the same order as the controller files.

## Common mistakes to avoid

- **Updating the code, "documenting later".** The README is part of the contract — there is no later.
- **Copying a similar section and forgetting to change fields.** The Expenses block is rich (status, paidAt) and is easy to over-copy when documenting a simpler aggregate like Incomes. Match the *actual* DTOs.
- **Documenting a status code that no handler emits.** Every entry in the Errors table must correspond to an `@ExceptionHandler` in `ApiExceptionHandler` (or to validation/auth handled by Spring itself).
- **Leaving stale request examples after a field rename.** Search the README for the old name before committing.
