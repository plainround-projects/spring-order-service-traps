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
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

/**
 * With the configured {@code open-in-view=false}, a lazy load in the response step fails where it
 * can be seen.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ImportTestcontainers(PostgresContainer.class)
@Import(LazyEndpointConfig.class)
class OpenInViewIT {

    @Value("${local.server.port}")
    private int port;

    @Autowired private CustomerJpaRepository customers;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void clean() {
        DatabaseReset.truncate(jdbc);
    }

    @Test
    void aLazyLoadOutsideATransactionFailsWhenOpenInViewIsOff() {
        long orderId = seedOrderWithTwoLines();

        int status = RestClientSupport.statusOf(port, "/test/lazy/" + orderId);

        assertThat(status).isEqualTo(500);
        Evidence.record("lazy load in the response step, open-in-view=false: HTTP " + status);
    }

    private long seedOrderWithTwoLines() {
        CustomerEntity customer = new CustomerEntity("ada");
        OrderEntity order = new OrderEntity(Instant.parse("2026-10-27T10:00:00Z"));
        customer.addOrder(order);
        order.addLine(new OrderLineEntity("sku-1", 1));
        order.addLine(new OrderLineEntity("sku-2", 1));
        return customers.saveAndFlush(customer).getOrders().get(0).getId();
    }

    /** Small HTTP helper shared with the open-in-view-on variant. */
    static final class RestClientSupport {
        private RestClientSupport() {}

        static int statusOf(int port, String path) {
            return RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {})
                    .build()
                    .get()
                    .uri(path)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode()
                    .value();
        }
    }
}
