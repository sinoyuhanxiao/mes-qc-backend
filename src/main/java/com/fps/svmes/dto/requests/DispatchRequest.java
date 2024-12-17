package com.fps.svmes.dto.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DispatchRequest {

    public enum ScheduleType {
        SPECIFIC_DAYS, INTERVAL
    }

    @NotNull(message = "Schedule type cannot be null")
    private ScheduleType scheduleType;

    @Min(value = 1, message = "Interval minutes must be at least 1")
    private Integer intervalMinutes;

    @Min(value = 1, message = "Repeat count must be at least 1")
    private Integer repeatCount;

    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Invalid time format, must be HH:mm")
    private String timeOfDay;

    @NotNull(message = "Active status cannot be null")
    private Boolean active;

    @NotEmpty(message = "List of days cannot be empty for SPECIFIC_DAYS schedule")
    private List<String> specificDays;

    @NotEmpty(message = "List of form IDs cannot be empty")
    private List<Long> formIds;

    @NotEmpty(message = "List of personnel IDs cannot be empty")
    private List<Long> personnelIds;
}



