package dev.plainround.orders.traps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.plainround.orders.infrastructure.persistence.CustomerEntity;
import dev.plainround.orders.infrastructure.persistence.CustomerJpaRepository;
import dev.plainround.orders.infrastructure.persistence.OrderJpaRepository;
import dev.plainround.orders.support.DatabaseReset;
import dev.plainround.orders.support.Evidence;
import dev.plainround.orders.support.PostgresContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Trap 2: a {@code @Transactional} method called from its own class. This class is deliberately not
 * {@code @Transactional}: a test-wide transaction would do the rolling back and hide the bug.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ImportTestcontainers(PostgresContainer.class)
@Import({SelfInvocationIT.Beans.class})
class SelfInvocationIT {

    static class Beans {
        @Bean
        SelfInvokingCheckout selfInvokingCheckout(
                CustomerJpaRepository customers, OrderJpaRepository orders) {
            return new SelfInvokingCheckout(customers, orders);
        }
    }

    @Autowired private SelfInvokingCheckout checkout;
    @Autowired private CustomerJpaRepository customers;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void clean() {
        DatabaseReset.truncate(jdbc);
    }

    @Test
    void selfInvocationLeavesAPartialOrderOnFailure() {
        Long customerId = customers.save(new CustomerEntity("ada")).getId();

        assertThatThrownBy(() -> checkout.checkoutSelfInvocationBug(customerId, true))
                .isInstanceOf(IllegalStateException.class);

        assertThat(DatabaseReset.count(jdbc, "orders_table")).isEqualTo(1);
        assertThat(DatabaseReset.count(jdbc, "order_lines")).isEqualTo(1);
        Evidence.record(
                "orders after failed self-invocation checkout: "
                        + DatabaseReset.count(jdbc, "orders_table")
                        + "   (a half-built order, persisted with no rollback)");
    }

    @Test
    void proxyFixedRollsBackCompletelyOnTheSameFailure() {
        Long customerId = customers.save(new CustomerEntity("ada")).getId();

        assertThatThrownBy(() -> checkout.checkoutProxyFixed(customerId, true))
                .isInstanceOf(IllegalStateException.class);

        assertThat(DatabaseReset.count(jdbc, "orders_table")).isZero();
        assertThat(DatabaseReset.count(jdbc, "order_lines")).isZero();
        Evidence.record(
                "orders after failed proxy-fixed checkout:      "
                        + DatabaseReset.count(jdbc, "orders_table")
                        + "   (the whole transaction rolled back as one unit)");
    }

    @Test
    void proxyFixedCommitsTheWholeOrderWhenNothingFails() {
        Long customerId = customers.save(new CustomerEntity("ada")).getId();

        checkout.checkoutProxyFixed(customerId, false);

        assertThat(DatabaseReset.count(jdbc, "orders_table")).isEqualTo(1);
        assertThat(DatabaseReset.count(jdbc, "order_lines")).isEqualTo(2);
    }
}
