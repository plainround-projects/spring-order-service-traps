package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.application.CheckoutService;
import dev.plainround.orders.application.OrderQueryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HTTP face of checkout and of a customer's order list. */
@RestController
@RequestMapping("/customers/{customerId}/orders")
public class OrderController {

    private final CheckoutService checkout;
    private final OrderQueryService queries;

    /**
     * Wires the controller.
     *
     * @param checkout the checkout use case
     * @param queries the order queries
     */
    public OrderController(CheckoutService checkout, OrderQueryService queries) {
        this.checkout = checkout;
        this.queries = queries;
    }

    /**
     * Places an order for the customer.
     *
     * @param customerId the buyer
     * @param request the validated body
     * @return the stored order
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(
            @PathVariable long customerId, @Valid @RequestBody CheckoutRequest request) {
        return OrderResponse.from(checkout.checkout(customerId, request.toDomain()));
    }

    /**
     * Lists the customer's orders with their lines.
     *
     * @param customerId the buyer
     * @return the orders, oldest first
     */
    @GetMapping
    public List<OrderResponse> list(@PathVariable long customerId) {
        return queries.listOrders(customerId).stream().map(OrderResponse::from).toList();
    }
}
