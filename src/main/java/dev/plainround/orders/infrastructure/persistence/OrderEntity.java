package dev.plainround.orders.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** The {@code orders_table} table ("order" is a reserved SQL word). Both sides are lazy. */
@Entity
@Table(name = "orders_table")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne is eager by default; lazy is stated, so a load of an order never drags the
    // customer in.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;

    private Instant placedAt;

    // Cascade follows ownership: the order owns its lines, the lines do not cascade back up.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<OrderLineEntity> lines = new ArrayList<>();

    /** For JPA. */
    protected OrderEntity() {}

    /**
     * Creates an order that is not stored yet.
     *
     * @param placedAt when the order was placed
     */
    public OrderEntity(Instant placedAt) {
        this.placedAt = placedAt;
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
     * Returns the buyer, loaded on first access.
     *
     * @return the customer
     */
    public CustomerEntity getCustomer() {
        return customer;
    }

    /**
     * Sets the buyer, the side that owns the foreign key.
     *
     * @param customer the buyer
     */
    public void setCustomer(CustomerEntity customer) {
        this.customer = customer;
    }

    /**
     * Returns when the order was placed.
     *
     * @return the instant
     */
    public Instant getPlacedAt() {
        return placedAt;
    }

    /**
     * Returns the lines, loaded on first access unless a fetch join or entity graph loaded them.
     *
     * @return the live collection
     */
    public List<OrderLineEntity> getLines() {
        return lines;
    }

    /**
     * Attaches a line and sets its owning side.
     *
     * @param line the line to attach
     */
    public void addLine(OrderLineEntity line) {
        line.setOrder(this);
        lines.add(line);
    }
}
