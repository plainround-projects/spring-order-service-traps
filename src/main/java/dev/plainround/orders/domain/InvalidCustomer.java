package dev.plainround.orders.domain;

/** A customer was built with values that break its invariants. */
public class InvalidCustomer extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param message which rule was broken
     */
    public InvalidCustomer(String message) {
        super(message);
    }
}
