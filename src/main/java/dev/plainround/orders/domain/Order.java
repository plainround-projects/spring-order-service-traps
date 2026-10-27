package dev.plainround.orders.domain;

import java.time.Instant;
import java.util.List;

/**
 * A stored order with its lines.
 *
 * @param id the identifier assigned by the store
 * @param customerId the buyer
 * @param placedAt when the order was placed
 * @param lines the lines, in the order they were given
 */
public record Order(long id, long customerId, Instant placedAt, List<OrderLine> lines) {

    /** Keeps an immutable copy of the lines. */
    public Order {
        lines = List.copyOf(lines);
    }
}
