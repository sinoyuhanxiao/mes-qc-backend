package com.fps.svmes.models.sql.task_schedule;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Represents a test dispatched to a specific personnel.
 */
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "dispatched_task", schema = "quality_management")
@Data
public class DispatchedTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dispatch_id", nullable = false)
    private Dispatch dispatch; // Associated configuration

    @Column(name = "personnel_id", nullable = false)
    private Long personnelId; // ID of the personnel receiving the test

    @Column(name = "form_id", nullable = false)
    private String formId; // ID of the dispatched form

    @Column(name = "dispatch_time", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime dispatchTime; // Time when the test was dispatched

    @Column(name = "status", length = 20, nullable = false)
    private String status; // Status of the test (e.g., "PENDING", "COMPLETED")

    @Column(name = "updated_at")
    @UpdateTimestamp
    private OffsetDateTime updatedAt; // Tracks when the record was last edited

    @Column(name = "notes")
    private String notes; // Optional notes about edits or status changes
}

