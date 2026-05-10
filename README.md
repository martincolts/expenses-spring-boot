# Expenses API

Spring Boot 4 REST API for managing multi-tenant expenses.

## Prerequisites

- Docker (for local Postgres)
- Java 26 (auto-provisioned by Gradle toolchain — no manual install needed)

## Getting started

```bash
# Copy the example env file (only needed once)
cp .env.example .env

# Start Postgres
docker compose up -d

# Run the application
./gradlew bootRun
```

The app starts on `http://localhost:8080`. Flyway runs migrations automatically on startup.

### Environment variables

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/expenses` | Postgres JDBC URL |
| `DB_USERNAME` | `postgres` | Postgres username |
| `DB_PASSWORD` | `postgres` | Postgres password |
| `JWT_SECRET` | *(dev placeholder)* | HS256 signing key — **must be overridden in production** |
| `JWT_EXPIRATION_MS` | `86400000` | Token lifetime in milliseconds (default: 24 h) |
| `ADMIN_NAME` | `Administrator` | Display name for the bootstrapped admin tenant |
| `ADMIN_EMAIL` | `admin@example.com` | Email/login for the bootstrapped admin tenant — **must be overridden in production** |
| `ADMIN_PASSWORD` | `adminpassword` | Password for the bootstrapped admin tenant — **must be overridden in production** |

> **Postgres ENUM note:** the default `DB_URL` includes `?stringtype=unspecified`. This is required so the JDBC driver can bind text parameters to the `expense_status` enum column. If you set `DB_URL` yourself (locally or in production), keep that query parameter. See [.claude/skills/postgres-enum-mapping](.claude/skills/postgres-enum-mapping/SKILL.md) for the full rationale.

### Running tests

```bash
./gradlew test
```

Tests spin up a real Postgres instance via Testcontainers — no manual database setup required.

---

## Quick start with curl

```bash
# 1. Register and capture the token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Acme Corp","email":"acme@example.com","password":"supersecret"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# 2. Call a protected endpoint
curl -s http://localhost:8080/api/tenants/1 \
  -H "Authorization: Bearer $TOKEN"

# 3. Reset your password
curl -s -X POST http://localhost:8080/api/auth/reset-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"currentPassword":"supersecret","newPassword":"newsecret1"}'
```

---

## Authentication

The API uses stateless JWT authentication. Protected endpoints require an `Authorization` header:

```
Authorization: Bearer <token>
```

Tokens are returned by the `/api/auth/register` and `/api/auth/login` endpoints.

### Roles

| Role | Granted by |
|---|---|
| `TENANT` | Automatically assigned on registration. The public `POST /api/auth/register` endpoint always creates `TENANT` accounts — there is no way to obtain `ADMIN` through the API. |
| `ADMIN` | Bootstrapped on application startup from the `ADMIN_NAME` / `ADMIN_EMAIL` / `ADMIN_PASSWORD` environment variables. Idempotent: if a tenant with `ADMIN_EMAIL` already exists, the bootstrap is skipped (the existing record is left untouched). |

To add additional admins after startup, update the `role` column directly:

```sql
UPDATE tenant SET role = 'ADMIN' WHERE email = 'user@example.com';
```

---

## API Reference

### Auth

#### Register

```
POST /api/auth/register
```

Creates a new account with the `TENANT` role and returns a JWT.

**Request body**

```json
{
  "name": "Acme Corp",
  "email": "acme@example.com",
  "password": "supersecret"
}
```

| Field | Type | Constraints |
|---|---|---|
| `name` | string | required, max 255 chars |
| `email` | string | required, valid email, max 255 chars |
| `password` | string | required, 8–255 chars |

**Response — `201 Created`**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "id": 1,
  "name": "Acme Corp",
  "email": "acme@example.com",
  "role": "TENANT"
}
```

**Example**

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Acme Corp","email":"acme@example.com","password":"supersecret"}'
```

**Errors**

| Status | Reason |
|---|---|
| `400` | Validation failed (missing/invalid fields) |
| `409` | Email already registered |

---

#### Login

```
POST /api/auth/login
```

Authenticates with email and password, returns a JWT.

**Request body**

```json
{
  "email": "acme@example.com",
  "password": "supersecret"
}
```

**Response — `200 OK`**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "id": 1,
  "name": "Acme Corp",
  "email": "acme@example.com",
  "role": "TENANT"
}
```

**Example**

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"acme@example.com","password":"supersecret"}'
```

**Errors**

| Status | Reason |
|---|---|
| `400` | Validation failed |
| `401` | Wrong email or password |

---

#### Reset password

```
POST /api/auth/reset-password
```

Changes the password of the authenticated user. Requires a valid JWT.

**Request body**

```json
{
  "currentPassword": "supersecret",
  "newPassword": "evenmoresecret"
}
```

| Field | Type | Constraints |
|---|---|---|
| `currentPassword` | string | required |
| `newPassword` | string | required, 8–255 chars |

**Response — `204 No Content`**

**Example**

```bash
curl -s -X POST http://localhost:8080/api/auth/reset-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"currentPassword":"supersecret","newPassword":"evenmoresecret"}'
```

**Errors**

| Status | Reason |
|---|---|
| `400` | Validation failed |
| `401` | Missing or invalid token |
| `422` | Current password is incorrect |

---

### Tenants

All tenant endpoints require a valid JWT. Access is role-restricted as described below.

#### List tenants

```
GET /api/tenants
```

**Required role:** `ADMIN`

**Response — `200 OK`**

```json
[
  {
    "id": 1,
    "name": "Acme Corp",
    "email": "acme@example.com",
    "role": "TENANT",
    "createdAt": "2026-05-10T12:00:00Z"
  }
]
```

**Example**

```bash
curl -s http://localhost:8080/api/tenants \
  -H "Authorization: Bearer $TOKEN"
```

**Errors**

| Status | Reason |
|---|---|
| `401` | Missing or invalid token |
| `403` | Caller is not an admin |

---

#### Get tenant

```
GET /api/tenants/{id}
```

**Required role:** `ADMIN`, or `TENANT` accessing their own account.

**Response — `200 OK`**

```json
{
  "id": 1,
  "name": "Acme Corp",
  "email": "acme@example.com",
  "role": "TENANT",
  "createdAt": "2026-05-10T12:00:00Z"
}
```

**Example**

```bash
curl -s http://localhost:8080/api/tenants/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Errors**

| Status | Reason |
|---|---|
| `401` | Missing or invalid token |
| `403` | Caller is not admin and does not own this account |
| `404` | Tenant not found |

---

#### Update tenant

```
PUT /api/tenants/{id}
```

Updates the tenant's name. Other fields (email, password, role) are not changed by this endpoint.

**Required role:** `ADMIN`, or `TENANT` updating their own account.

**Request body**

```json
{
  "name": "Acme Corp Ltd"
}
```

| Field | Type | Constraints |
|---|---|---|
| `name` | string | required, max 255 chars |

**Response — `200 OK`**

```json
{
  "id": 1,
  "name": "Acme Corp Ltd",
  "email": "acme@example.com",
  "role": "TENANT",
  "createdAt": "2026-05-10T12:00:00Z"
}
```

**Example**

```bash
curl -s -X PUT http://localhost:8080/api/tenants/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Acme Corp Ltd"}'
```

**Errors**

| Status | Reason |
|---|---|
| `400` | Validation failed |
| `401` | Missing or invalid token |
| `403` | Caller is not admin and does not own this account |
| `404` | Tenant not found |

---

#### Delete tenant

```
DELETE /api/tenants/{id}
```

**Required role:** `ADMIN`

**Response — `204 No Content`**

**Example**

```bash
curl -s -X DELETE http://localhost:8080/api/tenants/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Errors**

| Status | Reason |
|---|---|
| `401` | Missing or invalid token |
| `403` | Caller is not an admin |
| `404` | Tenant not found |

---

### Categories

Categories belong to a tenant. Each `(tenant_id, name)` pair must be unique, and a `TENANT` only sees and manages their own categories. All endpoints require a valid JWT.

#### Create category

```
POST /api/categories
```

Creates a category owned by the authenticated tenant. The `tenant_id` is taken from the JWT — it cannot be set in the request body.

**Required role:** `TENANT`

**Request body**

```json
{
  "name": "Food"
}
```

| Field | Type | Constraints |
|---|---|---|
| `name` | string | required, max 50 chars |

**Response — `201 Created`**

```json
{
  "id": 1,
  "tenantId": 1,
  "name": "Food",
  "createdAt": "2026-05-10T12:00:00Z"
}
```

**Example**

```bash
curl -s -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Food"}'
```

**Errors**

| Status | Reason |
|---|---|
| `400` | Validation failed |
| `401` | Missing or invalid token |
| `403` | Caller is not a tenant (e.g. admins cannot create categories) |
| `409` | Category name already exists for this tenant |

---

#### List categories

```
GET /api/categories
```

Lists categories visible to the caller. A `TENANT` sees only their own categories; an `ADMIN` sees all categories across every tenant.

**Required role:** `TENANT` or `ADMIN`

**Response — `200 OK`**

```json
[
  {
    "id": 1,
    "tenantId": 1,
    "name": "Food",
    "createdAt": "2026-05-10T12:00:00Z"
  }
]
```

**Example**

```bash
curl -s http://localhost:8080/api/categories \
  -H "Authorization: Bearer $TOKEN"
```

**Errors**

| Status | Reason |
|---|---|
| `401` | Missing or invalid token |

---

#### Get category

```
GET /api/categories/{id}
```

**Required role:** `ADMIN`, or `TENANT` accessing one of their own categories.

**Response — `200 OK`**

```json
{
  "id": 1,
  "tenantId": 1,
  "name": "Food",
  "createdAt": "2026-05-10T12:00:00Z"
}
```

**Example**

```bash
curl -s http://localhost:8080/api/categories/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Errors**

| Status | Reason |
|---|---|
| `401` | Missing or invalid token |
| `404` | Category not found, or owned by a different tenant |

---

#### Update category

```
PUT /api/categories/{id}
```

Renames a category. The owning tenant cannot be changed.

**Required role:** `TENANT`, and the category must belong to the caller.

**Request body**

```json
{
  "name": "Groceries"
}
```

| Field | Type | Constraints |
|---|---|---|
| `name` | string | required, max 50 chars |

**Response — `200 OK`**

```json
{
  "id": 1,
  "tenantId": 1,
  "name": "Groceries",
  "createdAt": "2026-05-10T12:00:00Z"
}
```

**Example**

```bash
curl -s -X PUT http://localhost:8080/api/categories/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Groceries"}'
```

**Errors**

| Status | Reason |
|---|---|
| `400` | Validation failed |
| `401` | Missing or invalid token |
| `403` | Caller is not a tenant |
| `404` | Category not found, or owned by a different tenant |
| `409` | New name already exists for this tenant |

---

#### Delete category

```
DELETE /api/categories/{id}
```

**Required role:** `ADMIN`, or `TENANT` deleting one of their own categories.

**Response — `204 No Content`**

**Example**

```bash
curl -s -X DELETE http://localhost:8080/api/categories/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Errors**

| Status | Reason |
|---|---|
| `401` | Missing or invalid token |
| `404` | Category not found, or owned by a different tenant |

---

### Expenses

Expenses belong to a tenant and must reference one of that tenant's categories. An expense is created either as already paid (`status = PAID`, `paidAt` set) or as pending (`status = NOT_PAUD_YET`, `paidAt = null`); a pending expense can later be moved to paid via the dedicated endpoint.

> The `NOT_PAUD_YET` literal is a typo carried over from the `expense_status` Postgres enum in `V1__create_expense_table.sql`. Migrations are immutable, so the value is preserved as-is — both in the DB and in the API.

#### Create expense

```
POST /api/expenses
```

Creates an expense owned by the authenticated tenant. The `tenantId` is taken from the JWT and cannot be supplied in the body. The referenced `categoryId` must belong to the same tenant.

**Required role:** `TENANT`

**Request body**

```json
{
  "categoryId": 1,
  "description": "Lunch with client",
  "amount": 42.50,
  "currency": "USD",
  "incurredAt": "2026-05-09T13:00:00Z",
  "paid": true
}
```

| Field | Type | Constraints |
|---|---|---|
| `categoryId` | number | required; must reference a category owned by the caller |
| `description` | string | required, max 255 chars |
| `amount` | number | required, positive, up to 2 decimal places |
| `currency` | string | required, exactly 3 chars (ISO 4217); normalized to upper-case |
| `incurredAt` | string (ISO 8601) | required |
| `paid` | boolean | optional (default `false`); when `true`, status is `PAID` and `paidAt` is set to the server time |

**Response — `201 Created`**

```json
{
  "id": 1,
  "tenantId": 1,
  "categoryId": 1,
  "description": "Lunch with client",
  "amount": 42.50,
  "currency": "USD",
  "status": "PAID",
  "incurredAt": "2026-05-09T13:00:00Z",
  "createdAt": "2026-05-10T12:00:00Z",
  "paidAt": "2026-05-10T12:00:00Z"
}
```

**Example**

```bash
curl -s -X POST http://localhost:8080/api/expenses \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"categoryId":1,"description":"Lunch with client","amount":42.50,"currency":"USD","incurredAt":"2026-05-09T13:00:00Z","paid":true}'
```

**Errors**

| Status | Reason |
|---|---|
| `400` | Validation failed |
| `401` | Missing or invalid token |
| `403` | Caller is not a tenant |
| `404` | `categoryId` does not exist or is owned by a different tenant |

---

#### List expenses

```
GET /api/expenses
```

Lists expenses ordered by `incurredAt` (newest first). A `TENANT` sees only their own expenses; an `ADMIN` sees all expenses across every tenant.

**Required role:** `TENANT` or `ADMIN`

**Response — `200 OK`**

```json
[
  {
    "id": 1,
    "tenantId": 1,
    "categoryId": 1,
    "description": "Lunch with client",
    "amount": 42.50,
    "currency": "USD",
    "status": "PAID",
    "incurredAt": "2026-05-09T13:00:00Z",
    "createdAt": "2026-05-10T12:00:00Z",
    "paidAt": "2026-05-10T12:00:00Z"
  }
]
```

**Example**

```bash
curl -s http://localhost:8080/api/expenses \
  -H "Authorization: Bearer $TOKEN"
```

**Errors**

| Status | Reason |
|---|---|
| `401` | Missing or invalid token |

---

#### Get expense

```
GET /api/expenses/{id}
```

**Required role:** `ADMIN`, or `TENANT` accessing one of their own expenses.

**Response — `200 OK`** (same shape as in the list response)

**Example**

```bash
curl -s http://localhost:8080/api/expenses/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Errors**

| Status | Reason |
|---|---|
| `401` | Missing or invalid token |
| `404` | Expense not found, or owned by a different tenant |

---

#### Update expense

```
PUT /api/expenses/{id}
```

Updates the editable fields of an expense. Status and `paidAt` are not changed by this endpoint — use `POST /api/expenses/{id}/pay` to flip status.

**Required role:** `TENANT`, and the expense must belong to the caller. The new `categoryId` must also belong to the caller.

**Request body**

```json
{
  "categoryId": 2,
  "description": "Lunch with client (corrected)",
  "amount": 45.00,
  "currency": "USD",
  "incurredAt": "2026-05-09T13:00:00Z"
}
```

| Field | Type | Constraints |
|---|---|---|
| `categoryId` | number | required; must reference a category owned by the caller |
| `description` | string | required, max 255 chars |
| `amount` | number | required, positive |
| `currency` | string | required, exactly 3 chars |
| `incurredAt` | string (ISO 8601) | required |

**Response — `200 OK`** (full expense, same shape as create)

**Example**

```bash
curl -s -X PUT http://localhost:8080/api/expenses/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"categoryId":2,"description":"Lunch with client (corrected)","amount":45.00,"currency":"USD","incurredAt":"2026-05-09T13:00:00Z"}'
```

**Errors**

| Status | Reason |
|---|---|
| `400` | Validation failed |
| `401` | Missing or invalid token |
| `403` | Caller is not a tenant |
| `404` | Expense not found, or `categoryId` not owned by the caller |

---

#### Mark expense as paid

```
POST /api/expenses/{id}/pay
```

Transitions a pending expense to `PAID` and stamps `paidAt` with the current server time. Idempotency is **not** silent — calling this on an already-paid expense returns `409`.

**Required role:** `ADMIN`, or `TENANT` paying one of their own expenses.

**Response — `200 OK`** (full expense, with `status = "PAID"` and `paidAt` set)

**Example**

```bash
curl -s -X POST http://localhost:8080/api/expenses/1/pay \
  -H "Authorization: Bearer $TOKEN"
```

**Errors**

| Status | Reason |
|---|---|
| `401` | Missing or invalid token |
| `404` | Expense not found, or owned by a different tenant |
| `409` | Expense is already paid |

---

#### Mark expense as unpaid

```
POST /api/expenses/{id}/unpay
```

Reverts a paid expense back to `NOT_PAUD_YET` and clears `paidAt`. Calling this on an expense that is already not paid returns `409`.

**Required role:** `ADMIN`, or `TENANT` reverting one of their own expenses.

**Response — `200 OK`** (full expense, with `status = "NOT_PAUD_YET"` and `paidAt = null`)

**Example**

```bash
curl -s -X POST http://localhost:8080/api/expenses/1/unpay \
  -H "Authorization: Bearer $TOKEN"
```

**Errors**

| Status | Reason |
|---|---|
| `401` | Missing or invalid token |
| `404` | Expense not found, or owned by a different tenant |
| `409` | Expense is not currently paid |

---

#### Delete expense

```
DELETE /api/expenses/{id}
```

**Required role:** `ADMIN`, or `TENANT` deleting one of their own expenses.

**Response — `204 No Content`**

**Example**

```bash
curl -s -X DELETE http://localhost:8080/api/expenses/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Errors**

| Status | Reason |
|---|---|
| `401` | Missing or invalid token |
| `404` | Expense not found, or owned by a different tenant |

---

### Incomes

Incomes belong to a tenant. Each income records an amount and the timestamp it was registered. A `TENANT` only sees and manages their own incomes; an `ADMIN` can list, read, and delete any income.

#### Create income

```
POST /api/incomes
```

Creates an income owned by the authenticated tenant. The `tenantId` is taken from the JWT — it cannot be set in the request body. `createdAt` is stamped by the server.

**Required role:** `TENANT`

**Request body**

```json
{
  "amount": 1500.00
}
```

| Field | Type | Constraints |
|---|---|---|
| `amount` | number | required, positive, up to 2 decimal places |

**Response — `201 Created`**

```json
{
  "id": 1,
  "tenantId": 1,
  "amount": 1500.00,
  "createdAt": "2026-05-10T12:00:00Z"
}
```

**Example**

```bash
curl -s -X POST http://localhost:8080/api/incomes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount":1500.00}'
```

**Errors**

| Status | Reason |
|---|---|
| `400` | Validation failed (missing or non-positive amount) |
| `401` | Missing or invalid token |
| `403` | Caller is not a tenant (e.g. admins cannot create incomes) |

---

#### List incomes

```
GET /api/incomes
```

Lists incomes ordered by `createdAt` (newest first). A `TENANT` sees only their own incomes; an `ADMIN` sees all incomes across every tenant.

**Required role:** `TENANT` or `ADMIN`

**Response — `200 OK`**

```json
[
  {
    "id": 1,
    "tenantId": 1,
    "amount": 1500.00,
    "createdAt": "2026-05-10T12:00:00Z"
  }
]
```

**Example**

```bash
curl -s http://localhost:8080/api/incomes \
  -H "Authorization: Bearer $TOKEN"
```

**Errors**

| Status | Reason |
|---|---|
| `401` | Missing or invalid token |

---

#### Get income

```
GET /api/incomes/{id}
```

**Required role:** `ADMIN`, or `TENANT` accessing one of their own incomes.

**Response — `200 OK`** (same shape as in the list response)

**Example**

```bash
curl -s http://localhost:8080/api/incomes/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Errors**

| Status | Reason |
|---|---|
| `401` | Missing or invalid token |
| `404` | Income not found, or owned by a different tenant |

---

#### Delete income

```
DELETE /api/incomes/{id}
```

**Required role:** `ADMIN`, or `TENANT` deleting one of their own incomes.

**Response — `204 No Content`**

**Example**

```bash
curl -s -X DELETE http://localhost:8080/api/incomes/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Errors**

| Status | Reason |
|---|---|
| `401` | Missing or invalid token |
| `404` | Income not found, or owned by a different tenant |

---

## Error format

All errors use [RFC 7807 Problem Detail](https://www.rfc-editor.org/rfc/rfc7807):

```json
{
  "status": 404,
  "detail": "Tenant 99 not found"
}
```
