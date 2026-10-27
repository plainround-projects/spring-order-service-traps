package dev.plainround.orders.domain;

import java.util.List;

/** Port: the audit trail. Writing to it must survive the rollback of the transaction that asked. */
public interface AuditLog {

    /**
     * Writes one entry and commits it on its own, whatever happens to the caller's transaction.
     *
     * @param action what was attempted
     * @param detail who and what
     */
    void record(String action, String detail);

    /**
     * Returns the newest entries first.
     *
     * @param limit how many entries at most
     * @return the entries
     */
    List<AuditEntry> latest(int limit);
}
