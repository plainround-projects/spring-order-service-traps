package dev.plainround.orders.traps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.plainround.orders.application.CheckoutService;
import dev.plainround.orders.application.CustomerService;
import dev.plainround.orders.application.StockService;
import dev.plainround.orders.domain.CustomerNotFound;
import dev.plainround.orders.domain.Order;
import dev.plainround.orders.domain.OrderLine;
import dev.plainround.orders.domain.OutOfStock;
import dev.plainround.orders.domain.StockConflict;
import dev.plainround.orders.infrastructure.persistence.JpaStockRepository;
import dev.plainround.orders.infrastructure.persistence.StockItemEntity;
import dev.plainround.orders.infrastructure.persistence.StockJpaRepository;
import dev.plainround.orders.support.DatabaseReset;
import dev.plainround.orders.support.PostgresContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The fixed checkout, in production code, against real PostgreSQL: all of it or none of it, and no
 * overselling.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ImportTestcontainers(PostgresContainer.class)
class CheckoutIT {

    @Autowired private CheckoutService checkout;
    @Autowired private CustomerService customers;
    @Autowired private StockService stock;
    @Autowired private JpaStockRepository stockRepository;
    @Autowired private StockJpaRepository stockJpa;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void clean() {
        DatabaseReset.truncate(jdbc);
    }

    @Test
    void checkoutStoresTheOrderWithAllItsLinesAndTakesTheStock() {
        long customerId = customers.register("ada").id();
        stock.setAvailable("sku-1", 5);
        stock.setAvailable("sku-2", 2);

        Order placed =
                checkout.checkout(
                        customerId, List.of(new OrderLine("sku-1", 2), new OrderLine("sku-2", 1)));

        assertThat(placed.lines()).hasSize(2);
        assertThat(stock.available("sku-1")).isEqualTo(3);
        assertThat(stock.available("sku-2")).isEqualTo(1);
        assertThat(DatabaseReset.count(jdbc, "order_lines")).isEqualTo(2);
    }

    @Test
    void aLaterLineOutOfStockRollsBackTheOrderAndTheEarlierReservationButKeepsTheAudit() {
        long customerId = customers.register("ada").id();
        stock.setAvailable("sku-1", 5);
        stock.setAvailable("sku-2", 0);

        assertThatThrownBy(
                        () ->
                                checkout.checkout(
                                        customerId,
                                        List.of(
                                                new OrderLine("sku-1", 2),
                                                new OrderLine("sku-2", 1))))
                .isInstanceOf(OutOfStock.class);

        assertThat(DatabaseReset.count(jdbc, "orders_table")).isZero();
        assertThat(DatabaseReset.count(jdbc, "order_lines")).isZero();
        assertThat(stock.available("sku-1")).as("the earlier reservation rolled back").isEqualTo(5);
        assertThat(DatabaseReset.count(jdbc, "audit_records"))
                .as("the attempt is still on record")
                .isEqualTo(1);
    }

    @Test
    void anUnknownCustomerLeavesOnlyTheAuditEntry() {
        stock.setAvailable("sku-1", 5);

        assertThatThrownBy(() -> checkout.checkout(999, List.of(new OrderLine("sku-1", 1))))
                .isInstanceOf(CustomerNotFound.class);

        assertThat(DatabaseReset.count(jdbc, "orders_table")).isZero();
        assertThat(DatabaseReset.count(jdbc, "audit_records")).isEqualTo(1);
    }

    @Test
    void aTransactionThatReadAStaleStockVersionLosesTheRaceInsteadOfOverwriting() {
        stock.setAvailable("sku-1", 5);
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate inner = new TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        outer.executeWithoutResult(
                status -> {
                    StockItemEntity stale = stockJpa.findById("sku-1").orElseThrow();
                    inner.executeWithoutResult(other -> stockRepository.reserve("sku-1", 1));
                    assertThat(stale.getVersion()).isZero();

                    assertThatThrownBy(() -> stockRepository.reserve("sku-1", 1))
                            .isInstanceOf(StockConflict.class);
                    status.setRollbackOnly();
                });

        assertThat(stock.available("sku-1")).as("only the first reservation was kept").isEqualTo(4);
    }

    @Test
    void racingCheckoutsForTheLastUnitsNeverOversell() throws Exception {
        long customerId = customers.register("ada").id();
        stock.setAvailable("sku-race", 3);
        AtomicInteger sold = new AtomicInteger();
        AtomicInteger outOfStock = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> attempts = new ArrayList<>();
            for (int attempt = 0; attempt < 8; attempt++) {
                attempts.add(
                        pool.submit(
                                () -> {
                                    try {
                                        checkout.checkout(
                                                customerId, List.of(new OrderLine("sku-race", 1)));
                                        sold.incrementAndGet();
                                    } catch (OutOfStock exception) {
                                        outOfStock.incrementAndGet();
                                    } catch (StockConflict exception) {
                                        conflicts.incrementAndGet();
                                    }
                                }));
            }
            for (Future<?> attempt : attempts) {
                attempt.get();
            }
        }

        assertThat(sold.get() + outOfStock.get() + conflicts.get()).isEqualTo(8);
        assertThat(sold.get()).isBetween(1, 3);
        assertThat(stock.available("sku-race")).isEqualTo(3 - sold.get());
        assertThat(DatabaseReset.count(jdbc, "orders_table")).isEqualTo(sold.get());
    }
}
