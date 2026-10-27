package dev.plainround.orders.domain;

/** An order or one of its lines breaks a rule, such as an empty basket or the same SKU twice. */
public class InvalidOrder extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param message which rule was broken
     */
    public InvalidOrder(String message) {
        super(message);
    }
}
