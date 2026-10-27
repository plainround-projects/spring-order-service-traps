package dev.plainround.orders.domain;

import java.time.Instant;

/**
 * One line of the audit trail.
 *
 * @param id the identifier assigned by the store
 * @param action what was attempted, such as {@code CHECKOUT_ATTEMPT}
 * @param detail free text that says who and what, never a secret
 * @param recordedAt when the entry was written
 */
public record AuditEntry(long id, String action, String detail, Instant recordedAt) {}
