package com.fps.svmes.models.sql.task_schedule;

import com.fps.svmes.models.sql.user.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "qc_form_tree_node_id", nullable = false)
    private String qcFormTreeNodeId; // ID of the dispatched form

    @Column(name = "dispatch_time", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime dispatchTime; // Time when the test was dispatched

    @Column(name = "state", length = 20, nullable = false)
    private String state; // state of the test (e.g., "PENDING", "COMPLETED")

    @Column(name = "updated_at")
    @UpdateTimestamp
    private OffsetDateTime updatedAt; // Tracks when the record was last edited

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "notes")
    private String notes; // Optional notes about edits or status changes

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = true)
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "updated_by", nullable = true)
    private User updatedBy;

    @Column(name = "status", nullable = false, columnDefinition = "SMALLINT DEFAULT 1")
    private Integer status; // New column for active/inactive status
}

