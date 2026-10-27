package dev.plainround.orders.domain;

import java.util.Optional;

/** Port: the units on hand per SKU. */
public interface StockRepository {

    /**
     * Sets the units on hand, creating the record if the SKU is new.
     *
     * @param sku the product code
     * @param available the units on hand
     */
    void setAvailable(String sku, int available);

    /**
     * Reports the units on hand.
     *
     * @param sku the product code
     * @return the units, or empty if the SKU has no stock record
     */
    Optional<Integer> available(String sku);

    /**
     * Takes units out of stock as part of the caller's transaction.
     *
     * @param sku the product code
     * @param quantity how many units to take
     * @throws OutOfStock if the SKU has fewer units than asked for
     * @throws StockConflict if another checkout changed the same row first
     */
    void reserve(String sku, int quantity);
}
