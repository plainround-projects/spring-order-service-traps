package dev.plainround.orders;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import dev.plainround.orders.support.DatabaseReset;
import dev.plainround.orders.support.PostgresContainer;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

/** The lesson's whole scenario over HTTP against a real server and a real PostgreSQL. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ImportTestcontainers(PostgresContainer.class)
class OrderServiceScenarioIT {

    @Value("${local.server.port}")
    private int port;

    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void clean() {
        DatabaseReset.truncate(jdbc);
    }

    @Test
    void checkoutListFailAndAuditThroughTheApi() {
        long customerId =
                JsonPath.<Integer>read(post("/customers", "{\"name\":\"Ada\"}").getBody(), "$.id");
        put("/stock/sku-1", "{\"available\":5}");
        put("/stock/sku-2", "{\"available\":0}");

        ResponseEntity<String> placed =
                post(orders(customerId), "{\"lines\":[{\"sku\":\"sku-1\",\"quantity\":2}]}");
        assertThat(placed.getStatusCode().value()).isEqualTo(201);

        String listed = get(orders(customerId)).getBody();
        assertThat(JsonPath.<List<?>>read(listed, "$")).hasSize(1);
        assertThat(JsonPath.<Integer>read(listed, "$[0].lines[0].quantity")).isEqualTo(2);

        ResponseEntity<String> shortage =
                post(
                        orders(customerId),
                        "{\"lines\":[{\"sku\":\"sku-1\",\"quantity\":1},{\"sku\":\"sku-2\",\"quantity\":1}]}");
        assertThat(shortage.getStatusCode().value()).isEqualTo(409);
        assertThat(shortage.getHeaders().getContentType().toString())
                .startsWith("application/problem+json");
        assertThat(JsonPath.<String>read(shortage.getBody(), "$.title")).isEqualTo("Out of stock");

        assertThat(JsonPath.<List<?>>read(get(orders(customerId)).getBody(), "$"))
                .as("still one order")
                .hasSize(1);
        assertThat(JsonPath.<Integer>read(get("/stock/sku-1").getBody(), "$.available"))
                .isEqualTo(3);
        assertThat(JsonPath.<List<?>>read(get("/audit").getBody(), "$"))
                .as("both attempts are on record")
                .hasSize(2);
    }

    @Test
    void errorsAreProblemJsonAtTheBoundary() {
        ResponseEntity<String> badBody = post("/customers", "{\"name\":\"\"}");
        ResponseEntity<String> unknown = get("/customers/9999/orders");
        ResponseEntity<String> malformed = post("/customers", "{not json");
        ResponseEntity<String> repeatedSku =
                post(
                        orders(createCustomer()),
                        "{\"lines\":[{\"sku\":\"sku-1\",\"quantity\":1},{\"sku\":\"sku-1\",\"quantity\":1}]}");

        assertThat(badBody.getStatusCode().value()).isEqualTo(400);
        assertThat(JsonPath.<String>read(badBody.getBody(), "$.errors[0].field")).isEqualTo("name");
        assertThat(unknown.getStatusCode().value()).isEqualTo(404);
        assertThat(malformed.getStatusCode().value()).isEqualTo(400);
        assertThat(repeatedSku.getStatusCode().value()).isEqualTo(422);
        for (ResponseEntity<String> response : List.of(badBody, unknown, malformed, repeatedSku)) {
            assertThat(response.getHeaders().getContentType().toString())
                    .startsWith("application/problem+json");
        }
    }

    @Test
    void healthIsUpWithTheDatabaseBehindIt() {
        assertThat(get("/actuator/health").getBody()).contains("\"status\":\"UP\"");
        assertThat(get("/actuator/health/readiness").getBody()).contains("UP");
        assertThat(get("/actuator/health").getHeaders().getFirst("X-Request-Id")).isNotBlank();
    }

    private long createCustomer() {
        return JsonPath.<Integer>read(post("/customers", "{\"name\":\"Ada\"}").getBody(), "$.id");
    }

    private static String orders(long customerId) {
        return "/customers/" + customerId + "/orders";
    }

    private RestClient client() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {})
                .build();
    }

    private ResponseEntity<String> get(String path) {
        return client().get().uri(path).retrieve().toEntity(String.class);
    }

    private ResponseEntity<String> post(String path, String body) {
        return client().post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);
    }

    private ResponseEntity<String> put(String path, String body) {
        return client().put()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);
    }
}
