package dev.plainround.orders.traps;

import dev.plainround.orders.infrastructure.persistence.CustomerEntity;
import dev.plainround.orders.infrastructure.persistence.CustomerJpaRepository;
import dev.plainround.orders.infrastructure.persistence.OrderEntity;
import dev.plainround.orders.infrastructure.persistence.OrderJpaRepository;
import dev.plainround.orders.infrastructure.persistence.OrderLineEntity;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trap 2, planted: the code a reviewer sees in a pull request, and its fix beside it. Test code
 * only.
 */
class SelfInvokingCheckout {

    private final CustomerJpaRepository customers;
    private final OrderJpaRepository orders;

    SelfInvokingCheckout(CustomerJpaRepository customers, OrderJpaRepository orders) {
        this.customers = customers;
        this.orders = orders;
    }

    /**
     * THE BUG: no annotation here, and the call below is a same-object call that skips the proxy.
     */
    public void checkoutSelfInvocationBug(Long customerId, boolean failHalfway) {
        this.saveOrder(customerId, failHalfway);
    }

    /** Looks transactional, but is never wrapped when called from the method above. */
    @Transactional
    public void saveOrder(Long customerId, boolean failHalfway) {
        placeInTwoSteps(customerId, failHalfway);
    }

    /**
     * THE FIX: the public entry point carries the annotation, so every call gets one real
     * transaction.
     */
    @Transactional
    public void checkoutProxyFixed(Long customerId, boolean failHalfway) {
        placeInTwoSteps(customerId, failHalfway);
    }

    private void placeInTwoSteps(Long customerId, boolean failHalfway) {
        CustomerEntity customer = customers.findById(customerId).orElseThrow();
        OrderEntity order = new OrderEntity(Instant.parse("2026-10-27T10:00:00Z"));
        order.setCustomer(customer);
        orders.save(order);
        order.addLine(new OrderLineEntity("sku-1", 1));
        orders.save(order);
        if (failHalfway) {
            throw new IllegalStateException("simulated failure partway through checkout");
        }
        order.addLine(new OrderLineEntity("sku-2", 1));
        orders.save(order);
    }
}
