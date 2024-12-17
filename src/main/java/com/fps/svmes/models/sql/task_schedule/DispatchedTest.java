package com.fps.svmes.models.sql.task_schedule;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a test dispatched to a specific personnel.
 */
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "dispatched_test", schema = "quality_management")
@Data
public class DispatchedTest {
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
    private Long formId; // ID of the dispatched form

    @Column(name = "dispatch_time", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime dispatchTime; // Time when the test was dispatched

    @Column(name = "status", length = 20, nullable = false)
    private String status; // Status of the test (e.g., "PENDING", "COMPLETED")

    @Column(name = "last_updated")
    @UpdateTimestamp
    private LocalDateTime lastUpdated; // Tracks when the record was last edited

    @Column(name = "finished_at")
    private LocalDateTime finishedAt; // Timestamp when the test was marked as finished

    @Column(name = "notes")
    private String notes; // Optional notes about edits or status changes
}

