package com.fps.svmes.models.sql.taskSchedule;

import com.fps.svmes.models.sql.Common;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "dispatch", schema = "quality_management")
public class Dispatch extends Common {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "remark")
    private String remark;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Column(name = "dispatch_limit")
    private Integer dispatchLimit;

    @Column(name = "executed_count", nullable = false)
    private Integer executedCount = 0;

    @OneToMany(mappedBy = "dispatch", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<DispatchUser> dispatchUsers;

    @OneToMany(mappedBy = "dispatch", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<DispatchForm> dispatchForms;

    @Column(name = "due_date_offset_minute", nullable = false)
    private Integer dueDateOffsetMinute = 60; // Total offset in minutes, default to 1 hour

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;  // indicates whether the dispatch scheduled to assign forms for users

    public boolean isActiveAndWithinScheduledTime() {
        OffsetDateTime now = OffsetDateTime.now();
        return getStatus() == 1 &&
                startTime != null &&
                endTime != null &&
                !now.isBefore(startTime) && // now >= startTime
                !now.isAfter(endTime);     // now <= endTime
    }
}
