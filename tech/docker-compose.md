# Docker Compose

## What it is

Docker Compose runs several containers as one stack, described in one file, `docker-compose.yml`. The file lists **services** (each a container to run), their images or build instructions, ports, environment variables, and the order in which they start. `docker compose up -d --wait` starts everything in the background and waits until every service with a health check is healthy.

## Why this repository uses it

`make up` has to bring the whole system to a working state with one command, on any machine with Docker: PostgreSQL 18 and the service, with the service waiting for the database to be ready. The same file documents how the pieces connect.

## Where it appears

| Path | Role |
| --- | --- |
| `docker-compose.yml` | the stack |
| `.env` (made from `.env.example`) | values compose substitutes into the file (ports, the database password) |
| `Makefile` | `make up` runs `docker compose up -d --build --wait`; `make down` runs `docker compose down -v` |

## How it works underneath

1. Compose reads the YAML (see `yaml.md`) and the `.env` file next to it.
2. `${NAME:-default}` inside the file is replaced by the environment value or the default. `${NAME:?message}` fails with the message if the value is missing.
3. For a service with `build:`, compose builds the image from the Dockerfile; with `image:`, it pulls it.
4. Compose creates a private **network** for the stack. Each service is reachable by its **service name** as a host name, so inside the stack the service reaches the database at `postgres:5432`, not `localhost`.
5. `ports: "18101:8080"` publishes the container's port 8080 on the host's port 18101. Only published ports are reachable from your machine.
6. A `healthcheck:` is a command run inside the container at an interval. A service's `depends_on` with `condition: service_healthy` waits for it. `--wait` makes `up` return only when everything is healthy.
7. `docker compose down -v` stops and removes the containers, the network, and the named volumes (the data).

## Walkthrough of this repository's use

```yaml
# PostgreSQL 18 and the service. `make up` builds the image and waits until both are healthy.
services:
  postgres:
    image: postgres:18@sha256:edb0c08dc33befd0262ab3b5cf1c4f3f8cfd8b09e88c05b45adee29d6761ddb6
    environment:
      POSTGRES_USER: ${POSTGRES_USER:-orders}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?run make env, or set POSTGRES_PASSWORD in .env}
      POSTGRES_DB: ${POSTGRES_DB:-orders}
    ports:
      - "${POSTGRES_PORT:-15432}:5432"
    volumes:
      - pgdata:/var/lib/postgresql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
      interval: 3s
      timeout: 3s
      retries: 20

  order-service:
    build: .
    image: spring-order-service-traps:dev
    depends_on:
      postgres:
        condition: service_healthy
    ports:
      - "${APP_PORT:-18102}:8080"
    environment:
      POSTGRES_HOST: postgres
      POSTGRES_PORT: "5432"
      POSTGRES_USER: ${POSTGRES_USER:-orders}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?run make env, or set POSTGRES_PASSWORD in .env}
      POSTGRES_DB: ${POSTGRES_DB:-orders}
    read_only: true
    tmpfs:
      - /tmp
    cap_drop:
      - ALL
    security_opt:
      - no-new-privileges:true
    stop_grace_period: 30s
    healthcheck:
      # bash only: the JRE image has no curl. Passes when the readiness probe answers UP.
      test: ["CMD", "bash", "-c", "exec 3<>/dev/tcp/localhost/8080 && printf 'GET /actuator/health/readiness HTTP/1.0\\r\\n\\r\\n' >&3 && grep -q '\"status\":\"UP\"' <&3"]
      interval: 5s
      timeout: 3s
      retries: 30
      start_period: 15s

volumes:
  pgdata:
```

- `postgres:` uses the official image pinned by digest. `environment` gives the user, password and database; `${POSTGRES_PASSWORD:?run make env, or set POSTGRES_PASSWORD in .env}` fails fast with a message when the password is missing. `volumes: pgdata:/var/lib/postgresql` keeps the data in a named volume. The `healthcheck` runs `pg_isready`, a PostgreSQL tool that says whether the server accepts connections; `$$` is a literal `$` for the container's shell.
- `order-service:` is built from the Dockerfile. `depends_on: postgres: condition: service_healthy` starts it only after the database is ready. `POSTGRES_HOST: postgres` is the service name used as a host name.

- `read_only: true` makes the container's root filesystem read-only; `tmpfs: - /tmp` gives it one writable in-memory folder. `cap_drop: - ALL` removes every Linux capability (special privileges). `security_opt: - no-new-privileges:true` stops a process from gaining privileges. Together they are least privilege for the container.
- `stop_grace_period: 30s` is how long Docker waits after sending the stop signal before killing the process; it is longer than the application's 20 second graceful shutdown.
- `healthcheck: test: ["CMD", "bash", "-c", "..."]` runs bash in the container (the Java image has no `curl`): it opens a TCP connection to the application's own port with `exec 3<>/dev/tcp/localhost/8080`, writes an HTTP request, and `grep -q '"status":"UP"'` passes only on a healthy answer. `interval`, `timeout`, `retries` and `start_period` set the rhythm.

## Try it yourself

1. `make up` builds and starts the stack, then returns when it is healthy.
2. `docker compose ps` lists the services and their health.
3. `docker compose logs --tail 20 order-service` shows the service's JSON log.
4. `docker compose config` prints the file with every `${...}` filled in.
5. `make down` removes everything, including data volumes.

## Mistakes people make

- Using `localhost` inside a container to reach another service: use the service name.
- Forgetting that published ports clash with other programs: ports here are configurable in `.env`.
- `docker compose down` without `-v` and expecting a clean database.
- Tabs in YAML.

## Check yourself

1. How does one service find another? *By its service name on the compose network.*
2. What does `--wait` do? *Returns only when services with health checks are healthy.*
3. What does `${X:?msg}` do? *Fails with `msg` if `X` is not set.*
4. What does `read_only: true` do? *Makes the root filesystem unwritable.*
5. What does `down -v` remove that `down` keeps? *The named volumes.*

## Further reading

- Compose file reference: https://docs.docker.com/reference/compose-file/
- `docker compose up`: https://docs.docker.com/reference/cli/docker/compose/up/
