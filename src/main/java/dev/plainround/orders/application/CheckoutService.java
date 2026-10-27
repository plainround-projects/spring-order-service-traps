package dev.plainround.orders.application;

import dev.plainround.orders.domain.AuditLog;
import dev.plainround.orders.domain.Order;
import dev.plainround.orders.domain.OrderDraft;
import dev.plainround.orders.domain.OrderLine;
import dev.plainround.orders.domain.OrderRepository;
import dev.plainround.orders.domain.StockRepository;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Places an order: all of it, or none of it. */
@Service
public class CheckoutService {

    private static final Logger LOG = LoggerFactory.getLogger(CheckoutService.class);

    private final CustomerService customers;
    private final OrderRepository orders;
    private final StockRepository stock;
    private final AuditLog audit;
    private final Clock clock;

    /**
     * Wires the service.
     *
     * @param customers customer lookup
     * @param orders where orders are kept
     * @param stock the units on hand
     * @param audit the audit trail, a separate bean so its own transaction can commit independently
     * @param clock the time source
     */
    public CheckoutService(
            CustomerService customers,
            OrderRepository orders,
            StockRepository stock,
            AuditLog audit,
            Clock clock) {
        this.customers = customers;
        this.orders = orders;
        this.stock = stock;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * Checks out. The transaction starts here, on the public entry point, so every step below is
     * inside one unit of work: if a later line is out of stock, the order and the earlier
     * reservations roll back together. The audit entry is written first through another bean and
     * survives that rollback.
     *
     * @param customerId the buyer
     * @param lines what is being bought
     * @return the stored order
     */
    @Transactional
    public Order checkout(long customerId, List<OrderLine> lines) {
        audit.record(
                "CHECKOUT_ATTEMPT", "customer " + customerId + ", " + lines.size() + " line(s)");
        customers.require(customerId);
        Order placed = orders.place(new OrderDraft(customerId, lines), clock.instant());
        lines.forEach(line -> stock.reserve(line.sku(), line.quantity()));
        LOG.info("order placed id={} customerId={}", placed.id(), customerId);
        return placed;
    }
}
