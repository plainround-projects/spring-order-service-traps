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

The first line is a comment listing the main targets. `SHELL := /bin/bash` makes recipes run in bash (the default `sh` lacks some features). `MVN := ./mvnw -B -ntp` stores the Maven command with its flags once (`-B` batch mode, `-ntp` no download progress). `PORT ?=` lines are defaults. `-include .env` and `export` bring `.env` values in.

```make
help: ## list the targets
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | sed -E 's/:.*## /\t/' | sort

# Creates .env on first use and gives the database a random password; nothing secret is ever committed.
env:
```

The `help` target prints every target that has a `## comment` after its name: `grep` selects those lines, `sed` replaces `:.*## ` with a tab, `sort` orders them. That is why `make help` lists all targets with one-line descriptions.

```make
env:
	@[ -f .env ] || { cp .env.example .env; \
	  sed -i.bak "s|^POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=$$(openssl rand -hex 16)|" .env && rm .env.bak; \
	  echo "created .env with a random database password"; }

up: env ## build the image, start PostgreSQL and the service, wait until both are healthy
	docker compose up -d --build --wait

run: ## the lesson's scenario over HTTP against the running service (make up first)
	scripts/scenario.sh http://localhost:$(APP_PORT)

serve: env ## run the service from source on the host against the compose PostgreSQL (make up postgres only)
```

- `env` creates `.env` once and fills `POSTGRES_PASSWORD` with `openssl rand -hex 16`, a random 32-character string, using `sed` to replace the empty line; `$$(...)` is a shell command substitution (doubled dollar for make).
- `up: env` depends on `env`, then `docker compose up -d --build --wait` builds the image and waits for PostgreSQL and the service to be healthy.
- `run` runs `scripts/scenario.sh`.
```make
traps: ## run the trap tests and print the measured numbers (needs Docker for Testcontainers)
	@rm -f target/evidence.txt
	$(MVN) -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true \
	  -Dit.test='OrderModelIT,NPlusOneIT,SelfInvocationIT,AuditIT,OpenInView*IT' verify
	@echo; echo "== measured by the tests"; sort target/evidence.txt

db: ## create the scratch database orders_sql_demo from sql/001_schema.sql and sql/002_seed.sql
```
- `traps` runs only the trap tests (`-Dit.test=...` picks the classes, `-Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false` skips the unit tests, `-Djacoco.skip=true` skips coverage), then prints the numbers the tests wrote to `target/evidence.txt`.

```make
test: ## unit, slice and Testcontainers tests, then the coverage gate (needs Docker)
	$(MVN) verify

test-unit: ## unit and slice tests only; no Docker needed
	$(MVN) test

lint: ## Spotless format check and Checkstyle
```

`test` runs `./mvnw verify`: unit tests, integration tests and the coverage gates. `test-unit` runs only the fast tests.

```make
lint: ## Spotless format check and Checkstyle
	$(MVN) spotless:check checkstyle:check

format: ## apply the Spotless formatting
```

`lint` runs both style tools (see `spotless-google-java-format.md` and `checkstyle.md`).

```make
down: ## stop the containers and delete the database volume
	docker compose down -v --remove-orphans

clean: down
	$(MVN) clean
```

`down` stops the containers (`-v` also deletes their data volumes; `--remove-orphans` removes leftovers). `clean` depends on `down` and then deletes `target/` through Maven.

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
