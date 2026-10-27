package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.domain.Order;
import dev.plainround.orders.domain.OrderLine;
import java.time.Instant;
import java.util.List;

/**
 * An order as the API shows it.
 *
 * @param id the identifier
 * @param customerId the buyer
 * @param placedAt when the order was placed
 * @param lines the lines
 */
public record OrderResponse(long id, long customerId, Instant placedAt, List<LineResponse> lines) {

    /**
     * One line of an order.
     *
     * @param sku the product code
     * @param quantity the number of units
     */
    public record LineResponse(String sku, int quantity) {

        static LineResponse from(OrderLine line) {
            return new LineResponse(line.sku(), line.quantity());
        }
    }

    /**
     * Converts a domain order.
     *
     * @param order the order
     * @return its API shape
     */
    public static OrderResponse from(Order order) {
        List<LineResponse> lines = order.lines().stream().map(LineResponse::from).toList();
        return new OrderResponse(order.id(), order.customerId(), order.placedAt(), lines);
    }
}
