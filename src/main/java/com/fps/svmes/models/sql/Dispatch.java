package com.fps.svmes.models.sql;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents the configuration for dispatching QC tests.
 */
@Entity
@Data
public class Dispatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ScheduleType scheduleType; // Schedule type: SPECIFIC_DAYS or INTERVAL

    @ElementCollection
    private List<String> specificDays; // List of days (e.g., ["MON", "THU"])

    private String timeOfDay; // Time of day in HH:mm format

    private Integer intervalCount; // Number of intervals (e.g., every 2 units)

    @Enumerated(EnumType.STRING)
    private TimeUnit intervalUnit; // Unit of interval: MINUTES, HOURS, DAYS, WEEKS, MONTHS

    private Integer repeatCount; // Number of times to repeat the interval

    private LocalDateTime startTime; // Start time for interval-based schedules

    @ElementCollection
    private List<Long> formIds; // List of target form IDs to dispatch

    @ElementCollection
    private List<Long> targetPersonnel; // List of personnel IDs to dispatch tests to

    private boolean active; // Indicates if the configuration is active
}

