package dev.plainround.orders.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * The {@code stock_items} table; {@code version} makes concurrent updates of one row fail loudly.
 */
@Entity
@Table(name = "stock_items")
public class StockItemEntity {

    @Id private String sku;

    private int available;

    @Version private long version;

    /** For JPA. */
    protected StockItemEntity() {}

    /**
     * Creates a stock record.
     *
     * @param sku the product code
     * @param available the units on hand
     */
    public StockItemEntity(String sku, int available) {
        this.sku = sku;
        this.available = available;
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
     * Returns the units on hand.
     *
     * @return the units
     */
    public int getAvailable() {
        return available;
    }

    /**
     * Replaces the units on hand.
     *
     * @param available the new level
     */
    public void setAvailable(int available) {
        this.available = available;
    }

    /**
     * Returns the optimistic-lock version.
     *
     * @return the version
     */
    public long getVersion() {
        return version;
    }
}
