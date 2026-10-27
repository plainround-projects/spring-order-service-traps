package dev.plainround.orders.traps;

import static org.assertj.core.api.Assertions.assertThat;

import dev.plainround.orders.infrastructure.persistence.CustomerEntity;
import dev.plainround.orders.infrastructure.persistence.CustomerJpaRepository;
import dev.plainround.orders.infrastructure.persistence.OrderEntity;
import dev.plainround.orders.infrastructure.persistence.OrderJpaRepository;
import dev.plainround.orders.infrastructure.persistence.OrderLineEntity;
import dev.plainround.orders.support.Evidence;
import dev.plainround.orders.support.PostgresContainer;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.core.env.Environment;

/**
 * Steps 1 and 2: the model works on real PostgreSQL, and a test can count the statements Hibernate
 * ran.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportTestcontainers(PostgresContainer.class)
class OrderModelIT {

    @Autowired private CustomerJpaRepository customers;
    @Autowired private OrderJpaRepository orders;
    @Autowired private EntityManager entityManager;
    @Autowired private Environment environment;

    @Test
    void savesAndReadsBackACustomerWithTwoOrdersOfThreeLinesEach() {
        CustomerEntity customer = new CustomerEntity("ada");
        for (int orderIndex = 0; orderIndex < 2; orderIndex++) {
            OrderEntity order = new OrderEntity(Instant.parse("2026-10-27T10:00:00Z"));
            customer.addOrder(order);
            for (int lineIndex = 0; lineIndex < 3; lineIndex++) {
                order.addLine(new OrderLineEntity("sku-" + orderIndex + "-" + lineIndex, 1));
            }
        }
        customers.saveAndFlush(customer);
        entityManager.clear();

        CustomerEntity reloaded = customers.findById(customer.getId()).orElseThrow();

        assertThat(reloaded.getOrders()).hasSize(2);
        assertThat(reloaded.getOrders().get(0).getLines()).hasSize(3);
    }

    @Test
    void loadingOneOrderByIdIsExactlyOneStatement() {
        CustomerEntity customer = new CustomerEntity("ada");
        OrderEntity order = new OrderEntity(Instant.parse("2026-10-27T10:00:00Z"));
        customer.addOrder(order);
        order.addLine(new OrderLineEntity("sku-1", 1));
        customers.saveAndFlush(customer);
        entityManager.clear();
        Statistics statistics =
                entityManager
                        .getEntityManagerFactory()
                        .unwrap(SessionFactory.class)
                        .getStatistics();
        statistics.clear();

        orders.findById(order.getId());

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        Evidence.record(
                "loadingOneOrderByIdIsExactlyOneStatement: prepareStatementCount = "
                        + statistics.getPrepareStatementCount());
    }

    @Test
    void openInViewIsExplicitlyOffInTheApplicationConfiguration() {
        assertThat(environment.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
    }
}
