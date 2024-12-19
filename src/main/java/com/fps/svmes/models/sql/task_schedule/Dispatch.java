package com.fps.svmes.models.sql.task_schedule;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the configuration for dispatching QC tests.
 */

@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "dispatch", schema = "quality_management")
@Data
public class Dispatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @JsonManagedReference
    @OneToMany(mappedBy = "dispatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DispatchPersonnel> dispatchPersonnel = new ArrayList<>();

    @JsonManagedReference
    @OneToMany(mappedBy = "dispatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DispatchForm> dispatchForms = new ArrayList<>();

    @JsonManagedReference
    @OneToMany(mappedBy = "dispatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DispatchDay> dispatchDays = new ArrayList<>();

    @Column(name = "schedule_type")
    private String scheduleType;

    @Column(name = "interval_minutes")
    private Integer intervalMinutes;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "repeat_count")
    private Integer repeatCount;

    @Column(name = "executed_count", nullable = false)
    private Integer executedCount = 0;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "time_of_day")
    private String timeOfDay;

    @Override
    public String toString() {
        return "Dispatch{" +
                "id=" + id +
                ", scheduleType='" + scheduleType + '\'' +
                ", intervalMinutes=" + intervalMinutes +
                ", startTime=" + startTime +
                ", repeatCount=" + repeatCount +
                ", executedCount=" + executedCount +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", timeOfDay='" + timeOfDay + '\'' +
                '}';
    }


}


