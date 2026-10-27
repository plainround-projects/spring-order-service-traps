package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.domain.OrderLine;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One line of a checkout request.
 *
 * @param sku the product code
 * @param quantity the number of units
 */
public record LineRequest(
        @NotBlank(message = "sku must not be blank")
                @Size(max = OrderLine.MAX_SKU_LENGTH, message = "sku must be at most 64 characters")
                String sku,
        @Min(value = 1, message = "quantity must be at least 1")
                @Max(value = OrderLine.MAX_QUANTITY, message = "quantity must be at most 1000")
                int quantity) {

    /**
     * Converts to the domain line.
     *
     * @return the line
     */
    public OrderLine toDomain() {
        return new OrderLine(sku, quantity);
    }
}
