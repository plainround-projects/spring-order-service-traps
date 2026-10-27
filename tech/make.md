# Make and the Makefile

## What it is

`make` is a command-line tool that runs named tasks defined in a file called `Makefile`. Each task is a **target**. A target has a **recipe**: the shell commands to run, one per line, each starting with a **tab** character. A target can list **prerequisites** (other targets that must run first).

## Why this repository uses it

The project rule is that a reader needs only a few commands: `make up`, `make run`, `make test`, `make lint`, `make down`. The Makefile hides long commands behind those names and is the same in every repository.

## Where it appears

| Path | Role |
| --- | --- |
| `Makefile` | all the targets |
| `.github/workflows/ci.yml` | CI runs `make lint`, `make test`, `make audit` and the image build |
| `.env` | variables that `make` reads (see below) |

## How it works underneath

1. `make up` finds the file `Makefile`, finds the target `up`, runs its prerequisites first, then runs each recipe line in a new shell.
2. A line starting with `@` is run without printing the command first. A line starting with `-` ignores a failing exit code. A trailing `\` continues the command on the next line.
3. `$(NAME)` expands a make variable. `NAME ?= value` sets a default that an environment variable or a command-line value can override. In a recipe, a shell variable needs a doubled dollar: `$$HOME`.
4. `.PHONY: up run test` says these names are tasks, not files. Without it, a file called `test` would make `make test` do nothing.
5. `-include .env` reads variable definitions from `.env` if the file exists. `export` makes every variable visible to the commands as environment variables.
6. If a command exits with a non-zero status, `make` stops and reports `Error N`.

## Walkthrough of this repository's use

First the whole top of the Makefile, before any target:

```make
# make up | run | traps | test | lint | down   (make help lists all)
SHELL := /bin/bash
MVN := ./mvnw -B -ntp
APP_PORT ?= 18102
POSTGRES_USER ?= orders
POSTGRES_DB ?= orders
PSQL := docker compose exec -T postgres psql -U $(POSTGRES_USER) -v ON_ERROR_STOP=1 -q

-include .env
export

.PHONY: help env up run serve traps db queries psql health test test-unit lint format audit docker-build down clean

help: ## list the targets
```

- The first line is a comment (a line starting with `#`) listing the main targets.
- `SHELL := /bin/bash` makes `make` run recipe lines with bash instead of the default `sh`, which lacks some features the recipes use. `:=` assigns a value once, when the file is read.
- `MVN := ./mvnw -B -ntp` stores the Maven command with its flags in a variable: `./mvnw` is the wrapper (see `maven.md`), `-B` is batch mode (no colors or interactive prompts), `-ntp` means no transfer progress (do not print a line for each downloaded file).
- Lines such as `APP_PORT ?= 18101` set a variable only if it has no value yet, so `APP_PORT=18999 make up` overrides it. A **port** is the number a program listens on (see `properties-files.md`).
- `-include .env` reads variable definitions from the file `.env` if it exists (the leading `-` means "do not fail if it is missing"). `export` makes every variable visible to the commands that `make` starts, as environment variables.
- `.PHONY: ...` lists names that are tasks, not files. Without it, a file named `test` in the folder would make `make test` do nothing because that "target" would look up to date.
- `help:` begins the first target; its recipe is in the table below.

Every target in this Makefile, with its prerequisites, the exact commands, and what it does:

| Target | Runs first | Commands it runs | What it does |
| --- | --- | --- | --- |
| `help` | - | `@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) \| sed -E 's/:.*## /\t/' \| sort` | Prints every target that has a `## description` after its name. |
| `env` | - | `@[ -f .env ] \|\| { cp .env.example .env; \`<br>`sed -i.bak "s\|^POSTGRES_PASSWORD=.*\|POSTGRES_PASSWORD=$$(openssl rand -hex 16)\|" .env && rm .env.bak; \`<br>`echo "created .env with a random database password"; }` | Creates `.env` from `.env.example` if it does not exist yet. `.env` holds local settings such as ports. |
| `up` | env | `docker compose up -d --build --wait` | Builds the images and starts the containers, then waits until they report healthy. |
| `run` | - | `scripts/scenario.sh http://localhost:$(APP_PORT)` | Runs `scripts/scenario.sh`: the lesson's requests against the running service. |
| `serve` | env | `docker compose up -d --wait postgres`<br>`SERVER_PORT=$(APP_PORT) $(MVN) spring-boot:run` | Runs the application from source on your machine, in the foreground, without Docker for the application itself. |
| `traps` | - | `@rm -f target/evidence.txt`<br>`$(MVN) -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true \`<br>`-Dit.test='OrderModelIT,NPlusOneIT,SelfInvocationIT,AuditIT,OpenInView*IT' verify`<br>`@echo; echo "== measured by the tests"; sort target/evidence.txt` | Runs the trap tests only and prints the numbers they measured. |
| `db` | - | `-docker compose exec -T postgres dropdb -U $(POSTGRES_USER) --if-exists orders_sql_demo`<br>`docker compose exec -T postgres createdb -U $(POSTGRES_USER) orders_sql_demo`<br>`$(PSQL) -d orders_sql_demo < sql/001_schema.sql`<br>`$(PSQL) -d orders_sql_demo < sql/002_seed.sql` | Creates a scratch database `orders_sql_demo` in the running PostgreSQL and loads the schema and seed scripts into it. |
| `queries` | - | `docker compose exec -T postgres psql -U $(POSTGRES_USER) -d orders_sql_demo -v ON_ERROR_STOP=1 < sql/003_queries.sql` | Runs `sql/003_queries.sql` against the scratch database. |
| `psql` | - | `docker compose exec postgres psql -U $(POSTGRES_USER) -d $(POSTGRES_DB)` | Opens an interactive SQL prompt (`psql`) on the service's database. |
| `health` | - | `curl -fsS http://localhost:$(APP_PORT)/actuator/health/readiness \| jq -e '.status == "UP"' >/dev/null && echo UP` | Asks the readiness endpoint and succeeds only if it says UP. |
| `test` | - | `$(MVN) verify` | Runs the whole Maven build: unit tests, integration tests and the coverage limits. |
| `test-unit` | - | `$(MVN) test` | Runs only the unit and slice tests, which need no Docker. |
| `lint` | - | `$(MVN) spotless:check checkstyle:check` | Checks formatting (Spotless) and style rules (Checkstyle); changes nothing. |
| `format` | - | `$(MVN) spotless:apply` | Rewrites the Java files into the project format. |
| `audit` | - | `scripts/audit.sh` | Runs the dependency vulnerability scan. |
| `docker-build` | - | `docker build -t spring-order-service-traps:dev .` | Builds the container image(s) only, without starting anything. |
| `down` | - | `docker compose down -v --remove-orphans` | Stops and removes the containers, networks and data volumes. |
| `clean` | down | `$(MVN) clean` | Runs `down`, then deletes Maven's `target/` folders. |

How to read the table: "Runs first" lists the prerequisites, so `up` with `env` runs `env` and then `up`. "Commands it runs" shows each recipe line as written; `$(NAME)` is replaced by a variable's value, and `$$` is a literal `$` handed to the shell. A recipe line starting with `@` is not echoed before it runs, and a line starting with `-` ignores a failing exit code (the exit code is the number a program returns when it ends: 0 means success).

Four recipe details that are easy to misread:

- `[ -f .env ] || { cp .env.example .env; ... }` is shell: `[ -f .env ]` tests whether the file exists; `||` runs the right side only if the test failed; `{ ...; }` groups several commands.
- A backslash at the end of a line continues the command on the next line.
- `$$(openssl rand -hex 16)` (in the PostgreSQL project) asks `openssl` for 16 random bytes written as 32 hexadecimal characters; `$$( ... )` is shell command substitution, doubled so `make` leaves it alone.
- `docker compose exec -T postgres psql ...` runs a command inside the running database container; `-T` turns off the terminal so input can be piped in with `<`.

## Try it yourself

1. `make help` prints every target with its description.
2. `make -n test` prints the commands `make test` would run without running them.
3. `make lint` runs the style checks.
4. `APP_PORT=18999 make -n serve` shows the port variable passed through.

## Mistakes people make

- Spaces instead of a tab before a recipe line: `*** missing separator`.
- Forgetting `.PHONY`: a file with the target's name stops the task from running.
- Using `$VAR` instead of `$$VAR` for a shell variable inside a recipe.
- Expecting each recipe line to remember the previous one: each line is a new shell, so `cd` does not carry over (join lines with `&&` or `;`).

## Check yourself

1. What starts a recipe line? *A tab.*
2. What does `.PHONY` mean? *The target is a task, not a file.*
3. What does `@` before a command do? *Hides the command echo.*
4. How do you write a shell variable inside a recipe? *Double the dollar: `$$VAR`.*
5. What does `up: env` mean? *`env` runs before `up`.*

## Further reading

- GNU Make manual: https://www.gnu.org/software/make/manual/make.html
