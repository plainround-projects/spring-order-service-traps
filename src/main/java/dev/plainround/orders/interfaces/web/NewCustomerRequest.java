package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.domain.Customer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The body of {@code POST /customers}.
 *
 * @param name the display name
 */
public record NewCustomerRequest(
        @NotBlank(message = "name must not be blank")
                @Size(
                        max = Customer.MAX_NAME_LENGTH,
                        message = "name must be at most 100 characters")
                String name) {}
