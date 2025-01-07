package com.fps.svmes.models.sql.task_schedule;

import com.fps.svmes.models.sql.Common;
import com.fps.svmes.models.sql.user.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Represents a test dispatched to a specific personnel.
 */
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "dispatched_task_test", schema = "quality_management")
@Data
public class DispatchedTask extends Common {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

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

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "due_date")
    private OffsetDateTime dueDate;

    @Column(name = "notes")
    private String notes; // Optional notes about edits or status changes

    @Column(name = "description")
    private String description;

    @Column(name = "dispatched_task_state_id", nullable = false)
    private Integer dispatchedTaskStateId;

    @Column(name = "is_overdue", nullable = false)
    private boolean isOverdue;

}

