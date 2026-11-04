# Changelog

All notable changes to this project are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Drawn architecture and flow figures in `docs/diagrams/`, light and dark, shown in the README.

## [0.1.0] - 2026-10-27

### Added

- The order service: customers, orders with lines, a checkout, a customer's order list with lines, stock levels and a read-only audit trail, over Spring Data JPA and PostgreSQL 18 with Flyway migrations.
- The three traps of the lesson, each planted in test code, caught by a test and fixed in production code: the N+1 listing (statement counts 21 and 201 against 1), the self-invoked `@Transactional` method, and the audit row lost to a rollback.
- Stretch work: optimistic locking on stock with a deterministic lost-update test and a concurrent no-oversell test; `open-in-view` turned back on in a test to show the lazy load it hides.
- `docs/REVIEW-CHECKLIST.md` with five lines, each naming its test, and `ReviewChecklistTest` that fails if a named test does not exist.
- ArchUnit rules for the dependency rule, entity privacy, no self-invocation of `@Transactional` methods and no `@Transactional` on private methods.
- `sql/`: the full schema script (proved equal to the migrations by `SchemaMirrorIT`), seed data, the lesson's queries and the mirrored migrations; `make db`, `make queries`.
- Spotless, Checkstyle, JaCoCo gates, a multi-stage non-root `Dockerfile`, `docker-compose.yml`, `Makefile`, GitHub Actions CI with a dependency audit, JSON logs, health probes, graceful shutdown.
