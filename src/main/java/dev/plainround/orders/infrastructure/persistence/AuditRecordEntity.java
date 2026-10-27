package dev.plainround.orders.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** The {@code audit_records} table. */
@Entity
@Table(name = "audit_records")
public class AuditRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;

    private String detail;

    private Instant recordedAt;

    /** For JPA. */
    protected AuditRecordEntity() {}

    /**
     * Creates an entry that is not stored yet.
     *
     * @param action what was attempted
     * @param detail who and what
     * @param recordedAt when it was written
     */
    public AuditRecordEntity(String action, String detail, Instant recordedAt) {
        this.action = action;
        this.detail = detail;
        this.recordedAt = recordedAt;
    }

    /**
     * Returns the generated id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns what was attempted.
     *
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * Returns who and what.
     *
     * @return the detail
     */
    public String getDetail() {
        return detail;
    }

    /**
     * Returns when the entry was written.
     *
     * @return the instant
     */
    public Instant getRecordedAt() {
        return recordedAt;
    }
}
