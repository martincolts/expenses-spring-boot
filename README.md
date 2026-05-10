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
| `TENANT` | Automatically assigned on registration |
| `ADMIN` | Must be set manually in the database |

To promote a user to admin, update the `role` column directly:

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

## Error format

All errors use [RFC 7807 Problem Detail](https://www.rfc-editor.org/rfc/rfc7807):

```json
{
  "status": 404,
  "detail": "Tenant 99 not found"
}
```
