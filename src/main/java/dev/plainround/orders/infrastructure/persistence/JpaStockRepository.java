package dev.plainround.orders.infrastructure.persistence;

import dev.plainround.orders.domain.OutOfStock;
import dev.plainround.orders.domain.StockConflict;
import dev.plainround.orders.domain.StockRepository;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

/** Adapter: the stock port on top of Spring Data JPA, with optimistic locking on every change. */
@Repository
public class JpaStockRepository implements StockRepository {

    private final StockJpaRepository stock;

    /**
     * Wires the adapter.
     *
     * @param stock the Spring Data repository
     */
    public JpaStockRepository(StockJpaRepository stock) {
        this.stock = stock;
    }

    @Override
    public void setAvailable(String sku, int available) {
        StockItemEntity item =
                stock.findById(sku).orElseGet(() -> new StockItemEntity(sku, available));
        item.setAvailable(available);
        stock.save(item);
    }

    @Override
    public Optional<Integer> available(String sku) {
        return stock.findById(sku).map(StockItemEntity::getAvailable);
    }

    @Override
    public void reserve(String sku, int quantity) {
        StockItemEntity item = stock.findById(sku).orElseThrow(() -> new OutOfStock(sku));
        if (item.getAvailable() < quantity) {
            throw new OutOfStock(sku);
        }
        item.setAvailable(item.getAvailable() - quantity);
        flushOrConflict(item, sku);
    }

    private void flushOrConflict(StockItemEntity item, String sku) {
        try {
            stock.saveAndFlush(item);
        } catch (ObjectOptimisticLockingFailureException conflict) {
            throw new StockConflict(sku);
        }
    }
}
