package com.fps.svmes.dto.dtos.formAccessCalendar;

import com.fps.svmes.dto.dtos.CommonDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class CalendarAssignmentDTO extends CommonDTO {

    private Long id;

    private Integer teamId;

    private List<String> formTreeNodeIds;

    private String name;

    private OffsetDateTime startDate;

    private OffsetDateTime endDate;

    private OffsetDateTime startDateRecur;

    private OffsetDateTime endDateRecur;

    private OffsetTime startTime;

    private OffsetTime endTime;

    private Boolean allDay;

    private Integer groupId;

    private Integer[] daysOfWeek;
}
