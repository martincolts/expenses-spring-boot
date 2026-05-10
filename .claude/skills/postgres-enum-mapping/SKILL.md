---
name: postgres-enum-mapping
description: Use when adding, reading, or writing a column whose type is a Postgres native ENUM in this Spring Boot + Spring Data JDBC project. Triggers on phrases like "create an enum type", "add a status column", "CREATE TYPE … AS ENUM", "map this enum to Java", or anytime a migration declares `CREATE TYPE x AS ENUM (...)` or a column uses such a type. Critical because Spring Data JDBC + Postgres enums fail by default with a `column "x" is of type ... but expression is of type character varying` error unless the JDBC URL is configured correctly.
---

# Mapping Postgres ENUMs to Java with Spring Data JDBC

This project uses **Postgres native ENUM types** (e.g. `expense_status` in `V1__create_expense_table.sql`) and **Spring Data JDBC** (not JPA). The two don't talk to each other out of the box. There is one well-tested fix in this repo and a couple of foot-guns to avoid.

## The default fails

By default, Spring Data JDBC converts a Java enum field to a `String` (via `name()`) and binds it as a JDBC `VARCHAR` parameter. PostgreSQL **does not implicitly cast `VARCHAR` to a custom enum type** — so any `INSERT` or `UPDATE` that touches the enum column blows up with:

```
ERROR: column "status" is of type expense_status but expression is of type character varying
  Hint: You will need to rewrite or cast the expression.
```

This shows up at runtime, not at compile time, and not in `@DataJdbcTest` against H2 (which is one of several reasons we don't use H2 — see [spring-test-slices](../spring-test-slices/SKILL.md)).

## The fix this project uses: `stringtype=unspecified`

Append `?stringtype=unspecified` to the JDBC URL. With that flag, the Postgres JDBC driver sends `String` parameters as type `UNSPECIFIED` (a.k.a. `unknown`), and Postgres is then willing to cast them to the right enum at the server side.

In [src/main/resources/application.yaml](../../../src/main/resources/application.yaml):

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/expenses?stringtype=unspecified}
```

**This is the project's standard.** Any new environment that overrides `DB_URL` (CI, staging, production, a teammate's `.env`) **must** include `?stringtype=unspecified`. Forgetting it produces silent dev success + production runtime failure.

### Side-effect to be aware of

`stringtype=unspecified` applies to *all* string parameters in the connection, not just enum columns. Postgres will type-resolve them server-side, which is usually what you want, but it does mean the driver no longer pre-rejects "wrong shape" strings — e.g. it's slightly easier to send `'not-a-date'` to a `date` column and get a server-side error instead of a driver-side one. In practice this hasn't been an issue here.

## Java side — naming must match the SQL literal exactly

When you declare:

```sql
CREATE TYPE expense_status AS ENUM ('PAID', 'NOT_PAUD_YET');
```

the Java enum **must** spell each constant the same way as the SQL literal:

```java
public enum ExpenseStatus {
    PAID,
    NOT_PAUD_YET   // typo preserved on purpose — matches the SQL value
}
```

`Enum.valueOf(name)` is case-sensitive. If the SQL has a typo (as `NOT_PAUD_YET` does), the Java enum has to carry the same typo until a follow-up migration renames the value. **Don't "fix" the Java spelling** without also writing a migration like:

```sql
ALTER TYPE expense_status RENAME VALUE 'NOT_PAUD_YET' TO 'NOT_PAID_YET';
```

…and updating every row's data if needed. Migrations are immutable (see [create-migration](../create-migration/SKILL.md)), so the rename always goes in a *new* migration, never an edit of the original.

## Default Spring Data JDBC enum conversion is fine

Once the URL flag is in place, you don't need a custom converter for the common case:

- Field type: declare it as the Java enum (`ExpenseStatus status`).
- Column: map normally; default snake_case mapping (`status`) is fine.
- Reads: Spring Data JDBC sees the column as text and calls `Enum.valueOf(...)`.
- Writes: Spring Data JDBC calls `name()` and binds as `String`; the JDBC URL flag lets Postgres cast it to the enum.

No `@WritingConverter` / `@ReadingConverter` / `JdbcCustomConversions` bean is needed for this.

## When to reach for a custom converter instead

Skip the URL flag and write converters only if:

- You can't set the JDBC URL flag for some reason (rare — usually a managed-platform constraint).
- You need the Java enum constant to differ from the SQL literal (e.g. you want `NotPaidYet` in Java).

In that case, add a `JdbcCustomConversions` bean in a `@Configuration` class with a `@WritingConverter` that returns a `org.postgresql.util.PGobject` set to the enum type. This requires moving `org.postgresql:postgresql` from `runtimeOnly` to `implementation` in `build.gradle` so `PGobject` is on the compile classpath. Most of the time, the URL flag is simpler — prefer it.

## Don't change the column type to `VARCHAR` to dodge the issue

It's tempting to ditch the Postgres enum and use `VARCHAR` + a `CHECK` constraint. Don't do this casually:

- The current schema is committed and applied; converting an existing enum column is a multi-step migration with downtime risk.
- Postgres enums give you a clean `\dT+`/introspection view and prevent typos at the DB layer.
- The URL flag fully solves the integration problem.

If a teammate proposes this, the answer is "stringtype=unspecified is already in place — what specifically isn't working?" before agreeing to a schema change.

## Checklist when adding a new enum column

1. Define the Postgres enum type in a Flyway migration: `CREATE TYPE x AS ENUM (...)`. See [create-migration](../create-migration/SKILL.md).
2. Add a Java enum in `com.tincho.expenses.model` with constants whose `name()` matches the SQL literals **exactly** (case, underscores, typos and all).
3. Use the Java enum directly as the field type on the entity record. No `@Column` override is needed unless the column name doesn't follow snake_case.
4. Verify `application.yaml`'s default `DB_URL` still has `?stringtype=unspecified`. If you're adding a new datasource bean, propagate the flag.
5. Confirm every place that sets `DB_URL` outside the repo (CI, deploy manifests, teammate `.env` files) carries the flag too. Add it to `.env.example` if it isn't already.
6. Cover both reads and writes in tests against Testcontainers Postgres — H2 has its own enum semantics and will hide the failure mode this skill exists to prevent.

## Common mistakes

- Setting `DB_URL` in production without `?stringtype=unspecified` → `INSERT`/`UPDATE` fails the moment the enum column is touched. Tests pass locally; prod breaks immediately.
- Trying to "fix" a SQL typo by changing only the Java enum constant → `Enum.valueOf` throws `IllegalArgumentException` on every read. Always pair Java rename with a SQL `RENAME VALUE` migration.
- Adding a `@WritingConverter` returning a `String` while expecting it to fix the cast — it doesn't, because the issue isn't the Java conversion, it's the JDBC parameter type. Either use `PGobject` or the URL flag (prefer the latter).
- Using H2 in tests "to keep things fast" and then being surprised when prod fails — Postgres enums don't exist in H2 the same way. Stick with Testcontainers Postgres for anything that touches an enum column.
