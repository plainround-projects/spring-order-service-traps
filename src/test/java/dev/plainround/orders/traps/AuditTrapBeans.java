package dev.plainround.orders.traps;

import dev.plainround.orders.domain.AuditLog;
import dev.plainround.orders.infrastructure.persistence.AuditJpaRepository;
import dev.plainround.orders.infrastructure.persistence.AuditRecordEntity;
import java.time.Clock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trap 3, planted three ways. Test code only; each variant fails a checkout after it wrote the
 * audit entry.
 */
@TestConfiguration(proxyBeanMethods = false)
class AuditTrapBeans {

    @Bean
    SameTransactionAudit sameTransactionAudit(AuditJpaRepository audits, Clock clock) {
        return new SameTransactionAudit(audits, clock);
    }

    @Bean
    RequiresNewAudit requiresNewAudit(AuditLog auditLog) {
        return new RequiresNewAudit(auditLog);
    }

    @Bean
    SelfInvokedRequiresNewAudit selfInvokedRequiresNewAudit(
            AuditJpaRepository audits, Clock clock) {
        return new SelfInvokedRequiresNewAudit(audits, clock);
    }

    /** THE BUG: the audit entry joins the checkout transaction and rolls back with it. */
    static class SameTransactionAudit {
        private final AuditJpaRepository audits;
        private final Clock clock;

        SameTransactionAudit(AuditJpaRepository audits, Clock clock) {
            this.audits = audits;
            this.clock = clock;
        }

        @Transactional
        public void checkoutAuditSameTx(boolean fail) {
            audits.save(
                    new AuditRecordEntity("CHECKOUT_ATTEMPT", "same transaction", clock.instant()));
            if (fail) {
                throw new IllegalStateException("checkout failed after the audit write");
            }
        }
    }

    /** THE FIX: the entry goes through another bean's proxy, where REQUIRES_NEW is applied. */
    static class RequiresNewAudit {
        private final AuditLog auditLog;

        RequiresNewAudit(AuditLog auditLog) {
            this.auditLog = auditLog;
        }

        @Transactional
        public void checkoutAuditRequiresNew(boolean fail) {
            auditLog.record("CHECKOUT_ATTEMPT", "separate bean");
            if (fail) {
                throw new IllegalStateException("checkout failed after the audit write");
            }
        }
    }

    /**
     * THE FALSE FIX: REQUIRES_NEW on a method of the same bean is ignored, for the same reason as
     * trap 2.
     */
    static class SelfInvokedRequiresNewAudit {
        private final AuditJpaRepository audits;
        private final Clock clock;

        SelfInvokedRequiresNewAudit(AuditJpaRepository audits, Clock clock) {
            this.audits = audits;
            this.clock = clock;
        }

        @Transactional
        public void checkoutAuditSelfInvoked(boolean fail) {
            this.writeAudit();
            if (fail) {
                throw new IllegalStateException("checkout failed after the audit write");
            }
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void writeAudit() {
            audits.save(new AuditRecordEntity("CHECKOUT_ATTEMPT", "same bean", clock.instant()));
        }
    }
}
