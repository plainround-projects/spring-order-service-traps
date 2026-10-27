package dev.plainround.orders.traps;

import static org.assertj.core.api.Assertions.assertThat;

import dev.plainround.orders.domain.Order;
import dev.plainround.orders.infrastructure.persistence.CustomerEntity;
import dev.plainround.orders.infrastructure.persistence.JpaOrderRepository;
import dev.plainround.orders.infrastructure.persistence.OrderEntity;
import dev.plainround.orders.infrastructure.persistence.OrderLineEntity;
import dev.plainround.orders.support.Evidence;
import dev.plainround.orders.support.PostgresContainer;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;

/**
 * Trap 1, the N+1: the naive listing grows with the number of orders, the fetch-joined one does
 * not. The statement count is asserted, not read off a log.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportTestcontainers(PostgresContainer.class)
@Import(JpaOrderRepository.class)
class NPlusOneIT {

    @Autowired private EntityManager entityManager;
    @Autowired private NaiveOrderJpaRepository naive;
    @Autowired private JpaOrderRepository fixed;

    private Statistics statistics;

    @BeforeEach
    void statistics() {
        statistics =
                entityManager
                        .getEntityManagerFactory()
                        .unwrap(SessionFactory.class)
                        .getStatistics();
    }

    @Test
    void theNaiveListingRunsOneQueryForTheOrdersAndOneMorePerOrderForItsLines() {
        long customerId = seedCustomerWithOrders(20);

        int lines =
                naive.findByCustomerId(customerId).stream()
                        .mapToInt(order -> order.getLines().size())
                        .sum();

        assertThat(lines).isEqualTo(60);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1 + 20);
        Evidence.record("naive queries for 20 orders:  " + statistics.getPrepareStatementCount());
    }

    @Test
    void theNaiveCountKeepsGrowingWith200Orders() {
        long customerId = seedCustomerWithOrders(200);

        naive.findByCustomerId(customerId).forEach(order -> order.getLines().size());

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1 + 200);
        Evidence.record("naive queries for 200 orders: " + statistics.getPrepareStatementCount());
    }

    @Test
    void fetchJoinLoadsTwentyOrdersWithTheirLinesInOneStatement() {
        long customerId = seedCustomerWithOrders(20);

        List<Order> orders = fixed.findByCustomerId(customerId);

        assertThat(orders).hasSize(20);
        assertThat(orders).allSatisfy(order -> assertThat(order.lines()).hasSize(3));
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
        Evidence.record("fixed queries for 20 orders:  " + statistics.getPrepareStatementCount());
    }

    @Test
    void fetchJoinKeepsQueryCountFlatAt200Orders() {
        long customerId = seedCustomerWithOrders(200);

        List<Order> orders = fixed.findByCustomerId(customerId);

        assertThat(orders).hasSize(200);
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
        Evidence.record("fixed queries for 200 orders: " + statistics.getPrepareStatementCount());
    }

    private long seedCustomerWithOrders(int orderCount) {
        CustomerEntity customer = new CustomerEntity("ada");
        for (int orderIndex = 0; orderIndex < orderCount; orderIndex++) {
            OrderEntity order =
                    new OrderEntity(Instant.parse("2026-10-27T10:00:00Z").plusSeconds(orderIndex));
            customer.addOrder(order);
            for (int lineIndex = 0; lineIndex < 3; lineIndex++) {
                order.addLine(new OrderLineEntity("sku-" + orderIndex + "-" + lineIndex, 1));
            }
        }
        entityManager.persist(customer);
        entityManager.flush();
        entityManager.clear();
        statistics.clear();
        return customer.getId();
    }
}
