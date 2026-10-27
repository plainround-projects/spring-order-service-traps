package dev.plainround.orders.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to stock records. */
public interface StockJpaRepository extends JpaRepository<StockItemEntity, String> {}
