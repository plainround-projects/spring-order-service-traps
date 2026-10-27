package dev.plainround.orders.domain;

/** Another checkout changed the same stock row first; the caller may retry the whole checkout. */
public class StockConflict extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param sku the SKU whose stock row changed underneath this checkout
     */
    public StockConflict(String sku) {
        super("stock for " + sku + " was changed by another checkout");
    }
}
