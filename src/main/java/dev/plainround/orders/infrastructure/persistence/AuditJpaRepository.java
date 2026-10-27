package dev.plainround.orders.infrastructure.persistence;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Spring Data access to the audit trail. */
public interface AuditJpaRepository extends JpaRepository<AuditRecordEntity, Long> {

    /**
     * Returns the newest entries first.
     *
     * @param page the size limit
     * @return the entries
     */
    @Query("select a from AuditRecordEntity a order by a.id desc")
    List<AuditRecordEntity> findLatest(Pageable page);
}
