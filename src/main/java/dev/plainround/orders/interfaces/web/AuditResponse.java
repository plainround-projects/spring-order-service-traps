package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.domain.AuditEntry;
import java.time.Instant;

/**
 * An audit entry as the API shows it.
 *
 * @param id the identifier
 * @param action what was attempted
 * @param detail who and what
 * @param recordedAt when it was written
 */
public record AuditResponse(long id, String action, String detail, Instant recordedAt) {

    /**
     * Converts a domain entry.
     *
     * @param entry the entry
     * @return its API shape
     */
    public static AuditResponse from(AuditEntry entry) {
        return new AuditResponse(entry.id(), entry.action(), entry.detail(), entry.recordedAt());
    }
}
