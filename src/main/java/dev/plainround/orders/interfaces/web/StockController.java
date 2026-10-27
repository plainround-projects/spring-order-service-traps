package dev.plainround.orders.interfaces.web;

import dev.plainround.orders.application.StockService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP face of the stock levels. */
@RestController
@RequestMapping("/stock/{sku}")
public class StockController {

    private final StockService service;

    /**
     * Wires the controller.
     *
     * @param service the stock use cases
     */
    public StockController(StockService service) {
        this.service = service;
    }

    /**
     * Sets the units on hand for a SKU.
     *
     * @param sku the product code
     * @param request the validated body
     * @return the new level
     */
    @PutMapping
    public StockResponse set(
            @PathVariable @NotBlank @Size(max = 64, message = "sku must be at most 64 characters")
                    String sku,
            @Valid @RequestBody StockRequest request) {
        service.setAvailable(sku, request.available());
        return new StockResponse(sku, request.available());
    }

    /**
     * Reports the units on hand for a SKU.
     *
     * @param sku the product code
     * @return the level
     */
    @GetMapping
    public StockResponse get(@PathVariable String sku) {
        return new StockResponse(sku, service.available(sku));
    }
}
