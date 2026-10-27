package dev.plainround.orders.application;

import dev.plainround.orders.domain.Order;
import dev.plainround.orders.domain.OrderRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads a customer's orders. */
@Service
public class OrderQueryService {

    private final CustomerService customers;
    private final OrderRepository orders;

    /**
     * Wires the service.
     *
     * @param customers customer lookup
     * @param orders where orders are kept
     */
    public OrderQueryService(CustomerService customers, OrderRepository orders) {
        this.customers = customers;
        this.orders = orders;
    }

    /**
     * Lists a customer's orders with their lines.
     *
     * @param customerId the buyer
     * @return the orders, oldest first
     */
    @Transactional(readOnly = true)
    public List<Order> listOrders(long customerId) {
        customers.require(customerId);
        return orders.findByCustomerId(customerId);
    }
}
