package dev.plainround.orders.infrastructure.persistence;

import dev.plainround.orders.domain.Order;
import dev.plainround.orders.domain.OrderDraft;
import dev.plainround.orders.domain.OrderLine;
import dev.plainround.orders.domain.OrderRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Adapter: the order port on top of Spring Data JPA. */
@Repository
public class JpaOrderRepository implements OrderRepository {

    private final OrderJpaRepository orders;
    private final CustomerJpaRepository customers;

    /**
     * Wires the adapter.
     *
     * @param orders the Spring Data repository for orders
     * @param customers the Spring Data repository for customers, used for a reference only
     */
    public JpaOrderRepository(OrderJpaRepository orders, CustomerJpaRepository customers) {
        this.orders = orders;
        this.customers = customers;
    }

    @Override
    public Order place(OrderDraft draft, Instant placedAt) {
        OrderEntity entity = new OrderEntity(placedAt);
        draft.lines()
                .forEach(line -> entity.addLine(new OrderLineEntity(line.sku(), line.quantity())));
        entity.setCustomer(customers.getReferenceById(draft.customerId()));
        return toDomain(orders.save(entity));
    }

    @Override
    public List<Order> findByCustomerId(long customerId) {
        return orders.findByCustomerIdWithLines(customerId).stream()
                .map(JpaOrderRepository::toDomain)
                .toList();
    }

    private static Order toDomain(OrderEntity entity) {
        List<OrderLine> lines =
                entity.getLines().stream()
                        .map(line -> new OrderLine(line.getSku(), line.getQuantity()))
                        .toList();
        return new Order(entity.getId(), entity.getCustomer().getId(), entity.getPlacedAt(), lines);
    }
}
