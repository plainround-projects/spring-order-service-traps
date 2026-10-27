package dev.plainround.orders.domain;

import java.time.Instant;
import java.util.List;

/** Port: where orders are kept. */
public interface OrderRepository {

    /**
     * Stores an order with all its lines.
     *
     * @param draft what is being bought
     * @param placedAt when the order is placed
     * @return the stored order with its id
     */
    Order place(OrderDraft draft, Instant placedAt);

    /**
     * Lists a customer's orders, oldest first, with their lines, in a number of queries that does
     * not depend on how many orders there are.
     *
     * @param customerId the buyer
     * @return the orders, empty if there are none
     */
    List<Order> findByCustomerId(long customerId);
}
