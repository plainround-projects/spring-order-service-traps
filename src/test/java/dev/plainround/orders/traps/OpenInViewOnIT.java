package dev.plainround.orders.traps;

import static org.assertj.core.api.Assertions.assertThat;

import dev.plainround.orders.infrastructure.persistence.CustomerEntity;
import dev.plainround.orders.infrastructure.persistence.CustomerJpaRepository;
import dev.plainround.orders.infrastructure.persistence.OrderEntity;
import dev.plainround.orders.infrastructure.persistence.OrderLineEntity;
import dev.plainround.orders.support.DatabaseReset;
import dev.plainround.orders.support.Evidence;
import dev.plainround.orders.support.PostgresContainer;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Stretch 3: the same endpoint with Spring Boot's default turned back on. The bug it hid now
 * answers 200.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jpa.open-in-view=true")
@ImportTestcontainers(PostgresContainer.class)
@Import(LazyEndpointConfig.class)
class OpenInViewOnIT {

    @Value("${local.server.port}")
    private int port;

    @Autowired private CustomerJpaRepository customers;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void clean() {
        DatabaseReset.truncate(jdbc);
    }

    @Test
    void openInViewOnHidesTheLazyLoadByKeepingTheSessionOpenForTheWholeRequest() {
        CustomerEntity customer = new CustomerEntity("ada");
        OrderEntity order = new OrderEntity(Instant.parse("2026-10-27T10:00:00Z"));
        customer.addOrder(order);
        order.addLine(new OrderLineEntity("sku-1", 1));
        long orderId = customers.saveAndFlush(customer).getOrders().get(0).getId();

        int status = OpenInViewIT.RestClientSupport.statusOf(port, "/test/lazy/" + orderId);

        assertThat(status).isEqualTo(200);
        Evidence.record(
                "lazy load in the response step, open-in-view=true:  HTTP "
                        + status
                        + "   (the bug is hidden)");
    }
}
