package dev.plainround.orders.interfaces.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * The body of {@code PUT /stock/{sku}}.
 *
 * @param available the units on hand
 */
public record StockRequest(
        @Min(value = 0, message = "available must be 0 or more")
                @Max(value = 1_000_000, message = "available must be at most 1000000")
                int available) {}
