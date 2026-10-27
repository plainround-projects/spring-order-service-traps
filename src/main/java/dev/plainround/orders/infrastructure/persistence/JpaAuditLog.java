package dev.plainround.orders.infrastructure.persistence;

import dev.plainround.orders.domain.AuditEntry;
import dev.plainround.orders.domain.AuditLog;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter: the audit trail. This is deliberately its own bean, not a method of the checkout
 * service. {@code REQUIRES_NEW} is applied by the Spring proxy around a bean, and a call from a
 * method of the same class never goes through that proxy, so there the annotation would be ignored
 * and the entry would roll back with the checkout it records. Callers reach this bean through its
 * proxy, so the entry commits on its own.
 */
@Component
public class JpaAuditLog implements AuditLog {

    private final AuditJpaRepository audits;
    private final Clock clock;

    /**
     * Wires the adapter.
     *
     * @param audits the Spring Data repository
     * @param clock the time source
     */
    public JpaAuditLog(AuditJpaRepository audits, Clock clock) {
        this.audits = audits;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String detail) {
        audits.save(new AuditRecordEntity(action, detail, clock.instant()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEntry> latest(int limit) {
        return audits.findLatest(PageRequest.of(0, limit)).stream()
                .map(
                        entity ->
                                new AuditEntry(
                                        entity.getId(),
                                        entity.getAction(),
                                        entity.getDetail(),
                                        entity.getRecordedAt()))
                .toList();
    }
}
