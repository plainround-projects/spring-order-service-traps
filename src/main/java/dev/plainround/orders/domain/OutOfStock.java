package dev.plainround.orders.domain;

/** A SKU has too few units left, or no stock record at all. */
public class OutOfStock extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param sku the SKU that cannot cover the quantity
     */
    public OutOfStock(String sku) {
        super("not enough stock for " + sku);
    }
}
