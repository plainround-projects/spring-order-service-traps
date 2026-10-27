# 0002. The audit trail commits in its own transaction, from its own bean

Date: 2026-10-27. Status: accepted.

## Context

The audit entry for a checkout attempt must exist even when the checkout fails and rolls back (trap 3). `Propagation.REQUIRES_NEW` starts an independent transaction, but Spring applies it through a proxy around the bean, and a call from a method of the same bean never goes through that proxy (trap 2's mechanism).

## Decision

`AuditLog` is a domain port. `JpaAuditLog` implements it as its own bean with `@Transactional(propagation = REQUIRES_NEW)` on `record`. `CheckoutService` calls it first, before any other write. The class comment on `JpaAuditLog` explains why it must stay a separate bean, and `AuditIT.requiresNewOnTheSameBeanIsIgnored` proves the false fix does not work.

## Alternatives considered

- `TransactionTemplate` with `PROPAGATION_REQUIRES_NEW` inside the checkout service: works without a second bean, but moves a framework object into the application layer and hides the intent from the type system.
- A transactional outbox plus a relay: the right design when the audit must also reach another system; more machinery than this project needs.

## Consequences

- The audit write takes a second connection for an instant; with a pool of 10 and short writes this is fine, and it means a pool of one would deadlock (documented here, not configured away).
- The audit row records the attempt, not the outcome; adding an outcome row would be a second `REQUIRES_NEW` call after the checkout decides.
