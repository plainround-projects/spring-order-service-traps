package dev.plainround.orders.domain;

/** No customer has the requested id. */
public class CustomerNotFound extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param id the id that matched nothing
     */
    public CustomerNotFound(long id) {
        super("no customer with id " + id);
    }
}
