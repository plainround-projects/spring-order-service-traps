package dev.plainround.orders.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** The {@code order_lines} table. */
@Entity
@Table(name = "order_lines")
public class OrderLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    private String sku;

    private int quantity;

    /** For JPA. */
    protected OrderLineEntity() {}

    /**
     * Creates a line that is not attached yet.
     *
     * @param sku the product code
     * @param quantity the number of units
     */
    public OrderLineEntity(String sku, int quantity) {
        this.sku = sku;
        this.quantity = quantity;
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
     * Returns the order this line belongs to.
     *
     * @return the order
     */
    public OrderEntity getOrder() {
        return order;
    }

    /**
     * Sets the order, the side that owns the foreign key.
     *
     * @param order the order
     */
    public void setOrder(OrderEntity order) {
        this.order = order;
    }

    /**
     * Returns the product code.
     *
     * @return the SKU
     */
    public String getSku() {
        return sku;
    }

    /**
     * Returns the number of units.
     *
     * @return the quantity
     */
    public int getQuantity() {
        return quantity;
    }
}
