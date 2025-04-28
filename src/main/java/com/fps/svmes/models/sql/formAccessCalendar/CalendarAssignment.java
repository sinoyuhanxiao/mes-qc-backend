package com.fps.svmes.models.sql.formAccessCalendar;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fps.svmes.models.sql.Common;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.List;

@Entity
@Table(name = "calendar_assignment", schema = "quality_management")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CalendarAssignment extends Common {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer teamId;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<CalendarAssignmentForm> forms;

    private String name;

    private OffsetDateTime startDate;

    private OffsetDateTime endDate;

    private OffsetDateTime startDateRecur;

    private OffsetDateTime endDateRecur;

    private OffsetTime startTime;

    private OffsetTime endTime;

    private Boolean allDay;

    private Integer groupId;

    @Column(name = "days_of_week", columnDefinition = "integer[]")
    private Integer[] daysOfWeek;
}