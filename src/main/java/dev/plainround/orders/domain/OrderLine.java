package dev.plainround.orders.domain;

/**
 * One line of an order.
 *
 * @param sku the product code, 1 to {@value #MAX_SKU_LENGTH} characters, not blank
 * @param quantity the number of units, 1 to {@value #MAX_QUANTITY}
 */
public record OrderLine(String sku, int quantity) {

    /** Longest accepted SKU. */
    public static final int MAX_SKU_LENGTH = 64;

    /** Largest quantity on one line. */
    public static final int MAX_QUANTITY = 1000;

    /** Checks the invariants. */
    public OrderLine {
        if (sku == null || sku.isBlank() || sku.length() > MAX_SKU_LENGTH) {
            throw new InvalidOrder("sku must be 1 to " + MAX_SKU_LENGTH + " characters");
        }
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new InvalidOrder("quantity must be between 1 and " + MAX_QUANTITY);
        }
    }
}
