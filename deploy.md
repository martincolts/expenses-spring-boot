# Deployment guide

## Architecture

The production stack runs three containers managed by Docker Compose:

```
Browser → Caddy :3030 → backend:8080 (Spring Boot + React SPA)
                              ↑
                          postgres:5432
```

- **postgres** — PostgreSQL 16, data persisted in a named volume.
- **backend** — Spring Boot jar that serves both the REST API (`/api/**`) and the React frontend as embedded static files. Built from `Dockerfile` at deploy time.
- **caddy** — Thin reverse proxy. Accepts connections on port 3030 and forwards everything to the backend. Config is in `Caddyfile`.

All three services communicate over an isolated internal Docker network; only port 3030 is exposed to the host.

## Prerequisites

- Docker Engine 24+ and Docker Compose v2 installed on the server.
- The repository cloned on the server (git pull is enough for updates).
- Port 3030 open in the server's firewall.

## First-time setup

### 1. Clone the repo

```bash
git clone <repo-url> expenses
cd expenses
```

### 2. Create the environment file

```bash
cp .env.example .env
```

Edit `.env` and fill in real values for every variable:

| Variable | Description |
|---|---|
| `POSTGRES_USER` | Database user |
| `POSTGRES_PASSWORD` | Database password — use something strong |
| `POSTGRES_DB` | Database name |
| `JWT_SECRET` | Signing key for JWT tokens — minimum 32 characters |
| `JWT_EXPIRATION_MS` | Token lifetime in ms (default: 86400000 = 24 h) |
| `ADMIN_EMAIL` | Email address of the seeded admin account |
| `ADMIN_PASSWORD` | Password of the seeded admin account |
| `ADMIN_NAME` | Display name of the admin (default: Administrator) |

> **Security:** never commit `.env` to version control. It is in `.gitignore`.

### 3. Build and start

```bash
docker compose -f docker-compose.prod.yaml up -d --build
```

Docker will:
1. Build the backend image (Node builds the React app, then Gradle packages everything into a single jar).
2. Pull the postgres and caddy images.
3. Start all three containers in dependency order (postgres → backend → caddy).

The first build takes several minutes because Gradle downloads dependencies. Subsequent builds are faster thanks to Docker layer caching.

### 4. Verify

```bash
docker compose -f docker-compose.prod.yaml ps
```

All three services should show `Up (healthy)` or `Up`. Then open `http://<server-ip>:3030` in a browser.

Check logs if something looks wrong:

```bash
docker compose -f docker-compose.prod.yaml logs -f backend
docker compose -f docker-compose.prod.yaml logs -f caddy
```

## Updating to a new version

```bash
git pull
docker compose -f docker-compose.prod.yaml up -d --build
```

Compose rebuilds only the backend image (caddy and postgres use fixed images and are not rebuilt). The backend container is replaced with zero manual intervention; Flyway runs any new migrations automatically on startup.

## Stopping and starting

```bash
# stop all containers (data is preserved)
docker compose -f docker-compose.prod.yaml down

# start again without rebuilding
docker compose -f docker-compose.prod.yaml up -d
```

## Wiping data

```bash
# remove containers AND the postgres data volume — data is lost
docker compose -f docker-compose.prod.yaml down -v
```

## Rotating secrets

1. Edit `.env` with the new values.
2. Restart the affected service:

```bash
docker compose -f docker-compose.prod.yaml up -d --force-recreate backend
```

For the JWT secret: all existing tokens are immediately invalidated, so all users will need to log in again.

## Changing the port

Edit `docker-compose.prod.yaml` and `Caddyfile`. In `docker-compose.prod.yaml` change the ports mapping:

```yaml
ports:
  - "NEW_PORT:3030"
```

In `Caddyfile` change the listen address:

```
:NEW_PORT {
    reverse_proxy backend:8080
}
```

Then recreate the caddy container:

```bash
docker compose -f docker-compose.prod.yaml up -d --force-recreate caddy
```
