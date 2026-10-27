package dev.plainround.orders.traps;

import dev.plainround.orders.infrastructure.persistence.OrderEntity;
import dev.plainround.orders.infrastructure.persistence.OrderJpaRepository;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * A test-only endpoint that touches a lazy collection while building the response, outside any
 * transaction, the way a controller that returns an entity would. It is a router function, not an
 * annotated controller, so no component scan can pick it up.
 */
@TestConfiguration(proxyBeanMethods = false)
class LazyEndpointConfig {

    @Bean
    RouterFunction<ServerResponse> lazyLines(OrderJpaRepository orders) {
        return RouterFunctions.route()
                .GET(
                        "/test/lazy/{id}",
                        request -> {
                            OrderEntity order =
                                    orders.findById(Long.parseLong(request.pathVariable("id")))
                                            .orElseThrow();
                            return ServerResponse.ok()
                                    .body(Map.of("lines", order.getLines().size()));
                        })
                .build();
    }
}
