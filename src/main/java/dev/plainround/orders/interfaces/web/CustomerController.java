package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.application.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HTTP face of the customers. */
@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService service;

    /**
     * Wires the controller.
     *
     * @param service the customer use cases
     */
    public CustomerController(CustomerService service) {
        this.service = service;
    }

    /**
     * Registers a customer.
     *
     * @param request the validated body
     * @return the stored customer
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse register(@Valid @RequestBody NewCustomerRequest request) {
        return CustomerResponse.from(service.register(request.name()));
    }

    /**
     * Returns a customer.
     *
     * @param id the customer id
     * @return the customer
     */
    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable long id) {
        return CustomerResponse.from(service.require(id));
    }
}
