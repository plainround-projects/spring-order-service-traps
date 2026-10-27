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
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | sed -E 's/:.*## /\t/' | sort

# Creates .env on first use and gives the database a random password; nothing secret is ever committed.
env:
	@[ -f .env ] || { cp .env.example .env; \
	  sed -i.bak "s|^POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=$$(openssl rand -hex 16)|" .env && rm .env.bak; \
	  echo "created .env with a random database password"; }

up: env ## build the image, start PostgreSQL and the service, wait until both are healthy
	docker compose up -d --build --wait

run: ## the lesson's scenario over HTTP against the running service (make up first)
	scripts/scenario.sh http://localhost:$(APP_PORT)

serve: env ## run the service from source on the host against the compose PostgreSQL (make up postgres only)
	docker compose up -d --wait postgres
	SERVER_PORT=$(APP_PORT) $(MVN) spring-boot:run

# Runs the three trap tests plus the stretch tests and prints the numbers they measured.
traps: ## run the trap tests and print the measured numbers (needs Docker for Testcontainers)
	@rm -f target/evidence.txt
	$(MVN) -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true \
	  -Dit.test='OrderModelIT,NPlusOneIT,SelfInvocationIT,AuditIT,OpenInView*IT' verify
	@echo; echo "== measured by the tests"; sort target/evidence.txt

db: ## create the scratch database orders_sql_demo from sql/001_schema.sql and sql/002_seed.sql
	-docker compose exec -T postgres dropdb -U $(POSTGRES_USER) --if-exists orders_sql_demo
	docker compose exec -T postgres createdb -U $(POSTGRES_USER) orders_sql_demo
	$(PSQL) -d orders_sql_demo < sql/001_schema.sql
	$(PSQL) -d orders_sql_demo < sql/002_seed.sql

queries: ## run sql/003_queries.sql against orders_sql_demo (make db first)
	docker compose exec -T postgres psql -U $(POSTGRES_USER) -d orders_sql_demo -v ON_ERROR_STOP=1 < sql/003_queries.sql

psql: ## open psql on the service database
	docker compose exec postgres psql -U $(POSTGRES_USER) -d $(POSTGRES_DB)

health: ## exit 0 when the service reports UP
	curl -fsS http://localhost:$(APP_PORT)/actuator/health/readiness | jq -e '.status == "UP"' >/dev/null && echo UP

test: ## unit, slice and Testcontainers tests, then the coverage gate (needs Docker)
	$(MVN) verify

test-unit: ## unit and slice tests only; no Docker needed
	$(MVN) test

lint: ## Spotless format check and Checkstyle
	$(MVN) spotless:check checkstyle:check

format: ## apply the Spotless formatting
	$(MVN) spotless:apply

audit: ## known-vulnerability scan of the dependencies (OSV-Scanner in a container)
	scripts/audit.sh

docker-build: ## build the container image only
	docker build -t spring-order-service-traps:dev .

down: ## stop the containers and delete the database volume
	docker compose down -v --remove-orphans

clean: down
	$(MVN) clean
