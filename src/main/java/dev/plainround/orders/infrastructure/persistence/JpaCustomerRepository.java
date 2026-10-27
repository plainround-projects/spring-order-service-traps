package dev.plainround.orders.infrastructure.persistence;

import dev.plainround.orders.domain.Customer;
import dev.plainround.orders.domain.CustomerRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adapter: the customer port on top of Spring Data JPA. */
@Repository
public class JpaCustomerRepository implements CustomerRepository {

    private final CustomerJpaRepository customers;

    /**
     * Wires the adapter.
     *
     * @param customers the Spring Data repository
     */
    public JpaCustomerRepository(CustomerJpaRepository customers) {
        this.customers = customers;
    }

    @Override
    public Customer register(String name) {
        CustomerEntity saved = customers.save(new CustomerEntity(name));
        return new Customer(saved.getId(), saved.getName());
    }

    @Override
    public Optional<Customer> findById(long id) {
        return customers.findById(id).map(entity -> new Customer(entity.getId(), entity.getName()));
    }
}
