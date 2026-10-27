package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.domain.Customer;

/**
 * A customer as the API shows it.
 *
 * @param id the identifier
 * @param name the display name
 */
public record CustomerResponse(long id, String name) {

    /**
     * Converts a domain customer.
     *
     * @param customer the customer
     * @return its API shape
     */
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.id(), customer.name());
    }
}
