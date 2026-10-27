package dev.plainround.orders.application;

import dev.plainround.orders.domain.OutOfStock;
import dev.plainround.orders.domain.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sets and reads the units on hand. */
@Service
public class StockService {

    private final StockRepository stock;

    /**
     * Wires the service.
     *
     * @param stock the units on hand
     */
    public StockService(StockRepository stock) {
        this.stock = stock;
    }

    /**
     * Sets the units on hand.
     *
     * @param sku the product code
     * @param available the units
     */
    @Transactional
    public void setAvailable(String sku, int available) {
        stock.setAvailable(sku, available);
    }

    /**
     * Reports the units on hand.
     *
     * @param sku the product code
     * @return the units
     * @throws OutOfStock if the SKU has no stock record
     */
    @Transactional(readOnly = true)
    public int available(String sku) {
        return stock.available(sku).orElseThrow(() -> new OutOfStock(sku));
    }
}
