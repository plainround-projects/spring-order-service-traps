package dev.plainround.orders.traps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.plainround.orders.support.DatabaseReset;
import dev.plainround.orders.support.Evidence;
import dev.plainround.orders.support.PostgresContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Trap 3: an audit record that disappears whenever the checkout it records fails. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ImportTestcontainers(PostgresContainer.class)
@Import(AuditTrapBeans.class)
class AuditIT {

    @Autowired private AuditTrapBeans.SameTransactionAudit sameTx;
    @Autowired private AuditTrapBeans.RequiresNewAudit requiresNew;
    @Autowired private AuditTrapBeans.SelfInvokedRequiresNewAudit selfInvoked;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void clean() {
        DatabaseReset.truncate(jdbc);
    }

    @Test
    void sameTransactionAuditIsLostWhenCheckoutFails() {
        long before = DatabaseReset.count(jdbc, "audit_records");

        assertThatThrownBy(() -> sameTx.checkoutAuditSameTx(true))
                .isInstanceOf(IllegalStateException.class);

        assertThat(before).isZero();
        assertThat(DatabaseReset.count(jdbc, "audit_records")).isZero();
        Evidence.record(
                "audit rows before=0 after (same-tx, checkout failed)="
                        + DatabaseReset.count(jdbc, "audit_records"));
    }

    @Test
    void auditWithRequiresNewSurvivesRollback() {
        assertThatThrownBy(() -> requiresNew.checkoutAuditRequiresNew(true))
                .isInstanceOf(IllegalStateException.class);

        assertThat(DatabaseReset.count(jdbc, "audit_records")).isEqualTo(1);
        Evidence.record(
                "audit rows before=0 after (REQUIRES_NEW, checkout failed)="
                        + DatabaseReset.count(jdbc, "audit_records"));
    }

    @Test
    void requiresNewOnTheSameBeanIsIgnored() {
        assertThatThrownBy(() -> selfInvoked.checkoutAuditSelfInvoked(true))
                .isInstanceOf(IllegalStateException.class);

        assertThat(DatabaseReset.count(jdbc, "audit_records")).isZero();
        Evidence.record(
                "audit rows after REQUIRES_NEW on the same bean, checkout failed="
                        + DatabaseReset.count(jdbc, "audit_records")
                        + "   (annotation ignored)");
    }
}
