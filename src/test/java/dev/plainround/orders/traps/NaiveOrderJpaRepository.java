package dev.plainround.orders.traps;

import dev.plainround.orders.infrastructure.persistence.OrderEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Trap 1, as a reviewer would see it: a derived query that looks harmless and loads the lines
 * lazily.
 */
public interface NaiveOrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * One query for the orders; every access to {@code getLines()} afterwards costs one more.
     *
     * @param customerId the buyer
     * @return the orders with lazy lines
     */
    List<OrderEntity> findByCustomerId(Long customerId);
}
