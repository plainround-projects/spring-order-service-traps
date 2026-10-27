package dev.plainround.orders.application;

import dev.plainround.orders.domain.Customer;
import dev.plainround.orders.domain.CustomerNotFound;
import dev.plainround.orders.domain.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registers customers and looks them up. */
@Service
public class CustomerService {

    private final CustomerRepository customers;

    /**
     * Wires the service.
     *
     * @param customers where customers are kept
     */
    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    /**
     * Registers a customer.
     *
     * @param name the display name
     * @return the stored customer
     */
    @Transactional
    public Customer register(String name) {
        Customer.requireName(name);
        return customers.register(name);
    }

    /**
     * Returns a customer or fails with a named error.
     *
     * @param id the customer id
     * @return the customer
     * @throws CustomerNotFound if there is none
     */
    @Transactional(readOnly = true)
    public Customer require(long id) {
        return customers.findById(id).orElseThrow(() -> new CustomerNotFound(id));
    }
}
