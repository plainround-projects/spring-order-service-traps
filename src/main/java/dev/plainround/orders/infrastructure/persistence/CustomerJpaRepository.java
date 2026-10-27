package dev.plainround.orders.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to customers. */
public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, Long> {}
