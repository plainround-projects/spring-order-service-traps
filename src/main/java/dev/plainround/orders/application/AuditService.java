package dev.plainround.orders.application;

import dev.plainround.orders.domain.AuditEntry;
import dev.plainround.orders.domain.AuditLog;
import java.util.List;
import org.springframework.stereotype.Service;

/** Reads the audit trail. */
@Service
public class AuditService {

    private final AuditLog audit;

    /**
     * Wires the service.
     *
     * @param audit the audit trail
     */
    public AuditService(AuditLog audit) {
        this.audit = audit;
    }

    /**
     * Returns the newest entries first.
     *
     * @param limit how many at most
     * @return the entries
     */
    public List<AuditEntry> latest(int limit) {
        return audit.latest(limit);
    }
}
