---
name: spring-data-jdbc-patterns
description: Use when modeling persistence in this project — creating entities, repositories, or aggregate relationships. Triggers on requests to "add an entity", "save to the database", "create a repository", "model a relationship between X and Y", or "load X with its Ys". Critical because this project uses Spring Data JDBC, not JPA — patterns differ in important ways.
---

# Spring Data JDBC patterns

This project uses **Spring Data JDBC**, not JPA/Hibernate. The mental model is different and getting it wrong leads to subtle bugs.

## Core differences from JPA

| Concept | JPA | Spring Data JDBC |
|---|---|---|
| Identity | Managed entity, dirty checking | Plain object, no session |
| Lazy loading | Yes (proxies) | **No.** Every load is eager. |
| Bidirectional refs | `@OneToMany` + `@ManyToOne` mappedBy | **One direction only — from aggregate root to children.** |
| `save()` semantics | Merges into persistence context | **DELETE + INSERT children**, or UPDATE root |
| Cascade | Configurable | **Always cascades** within an aggregate |
| Caching | First/second-level cache | None |

If you find yourself reaching for `@Entity`, `EntityManager`, `@OneToMany(mappedBy=…)`, `FetchType.LAZY`, or `@Transactional(readOnly=true)` for performance — stop. Those don't exist or don't help here.

## Aggregate roots

The fundamental unit of persistence is the **aggregate**: a root entity plus the objects loaded and saved with it as one transaction.

- A `Repository<T, ID>` exists for the aggregate **root** only — never for child entities.
- Children are referenced **by value** from the root (e.g., `List<LineItem>` inside `Order`), not by `@OneToMany`.
- Cross-aggregate references use the child's ID, not an object reference: `AggregateReference<Customer, Long>`, not `Customer customer`.
- Saving the root saves the whole aggregate. Loading the root loads the whole aggregate.

Wrong:
```java
class Order {
    @ManyToOne Customer customer;  // JPA-think — won't work
    @OneToMany List<LineItem> items;
}
```

Right:
```java
@Table("orders")
class Order {
    @Id Long id;
    AggregateReference<Customer, Long> customer;  // FK to another aggregate
    List<LineItem> items;                         // owned children, no @OneToMany needed
}
```

## Naming and mapping

- Default: `CamelCase` class → `snake_case` table, `camelCase` field → `snake_case` column.
- Override with `@Table("…")` and `@Column("…")` only when the default doesn't work.
- IDs: declare as `Long` (boxed) so `null` means "unsaved". Spring Data JDBC uses `id == null` to decide INSERT vs UPDATE.
- Reserved names (`order`, `user`) need explicit `@Table("orders")` to avoid SQL keyword clashes.

## Repository conventions

- Extend `ListCrudRepository<T, ID>` (returns `List`) over `CrudRepository` (returns `Iterable`) — almost always what you want.
- Add `PagingAndSortingRepository` only when you actually paginate.
- Derived queries work (`findByDescriptionContaining`), but for anything non-trivial use `@Query` with explicit SQL — you're closer to the database here than with JPA, lean into it.
- Modifying queries need `@Modifying` and run inside a transaction.

## Transactions

- Repository methods are transactional by default for writes.
- Wrap multi-step operations in `@Transactional` at the **service** layer, not the controller or repository.
- There is no `readOnly=true` performance benefit comparable to JPA; use it only for semantic clarity.

## When to drop to JdbcTemplate

Spring Data JDBC repositories are great for aggregate CRUD. For reporting queries, joins across aggregates, or projections, inject `JdbcTemplate` / `NamedParameterJdbcTemplate` and write SQL. Don't fight the repository abstraction with derived queries that span aggregates.

## Common mistakes to avoid

- Adding a getter for a `List<Child>` and calling `.add()` expecting it to persist — it won't auto-save. You must call `repository.save(root)`.
- Trying to load just the root without children — Spring Data JDBC will load the whole aggregate. If that's expensive, your aggregate boundary is wrong; split it.
- Using `Optional<T>` field types in entities — supported, but prefer nullable fields and let services wrap in `Optional` at the API boundary.
- Generating IDs in Java (`UUID.randomUUID()`) and then being surprised that `save()` does an UPDATE — Spring Data JDBC sees `id != null` and assumes existing row. For client-generated IDs, implement `Persistable<ID>` and override `isNew()`.
