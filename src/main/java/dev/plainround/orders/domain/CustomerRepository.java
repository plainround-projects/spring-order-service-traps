package dev.plainround.orders.domain;

import java.util.Optional;

/** Port: where customers are kept. */
public interface CustomerRepository {

    /**
     * Stores a new customer.
     *
     * @param name the validated display name
     * @return the stored customer with its id
     */
    Customer register(String name);

    /**
     * Looks a customer up.
     *
     * @param id the customer id
     * @return the customer, or empty if there is none
     */
    Optional<Customer> findById(long id);
}
