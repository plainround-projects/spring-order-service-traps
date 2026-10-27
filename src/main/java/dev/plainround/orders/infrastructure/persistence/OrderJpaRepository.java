package dev.plainround.orders.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data access to orders. */
public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * Loads a customer's orders and their lines in one SQL statement. The entity graph turns the
     * lazy {@code lines} collection into a join, so the statement count does not grow with the
     * number of orders.
     *
     * @param customerId the buyer
     * @return the orders, oldest first, with lines loaded
     */
    @EntityGraph(attributePaths = "lines")
    @Query("select o from OrderEntity o where o.customer.id = :customerId order by o.id")
    List<OrderEntity> findByCustomerIdWithLines(@Param("customerId") Long customerId);
}
