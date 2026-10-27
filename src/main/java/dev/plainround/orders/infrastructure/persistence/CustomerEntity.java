package dev.plainround.orders.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code customers} table. Orders load lazily: JPA's default for a collection, stated for the
 * reader.
 */
@Entity
@Table(name = "customers")
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // Deleting a customer deletes the orders: the customer owns their lifecycle.
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<OrderEntity> orders = new ArrayList<>();

    /** For JPA. */
    protected CustomerEntity() {}

    /**
     * Creates a customer that is not stored yet.
     *
     * @param name the display name
     */
    public CustomerEntity(String name) {
        this.name = name;
    }

    /**
     * Returns the generated id.
     *
     * @return the id, null before the first save
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the display name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the orders, loaded on first access.
     *
     * @return the live collection
     */
    public List<OrderEntity> getOrders() {
        return orders;
    }

    /**
     * Attaches an order and sets its owning side.
     *
     * @param order the order to attach
     */
    public void addOrder(OrderEntity order) {
        order.setCustomer(this);
        orders.add(order);
    }
}
