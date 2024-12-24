package com.fps.svmes.dto.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fps.svmes.utils.ValidDispatchRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ValidDispatchRequest
public class DispatchRequest {

    public enum ScheduleType {
        SPECIFIC_DAYS, INTERVAL
    }

    @JsonProperty("name")
    @NotNull(message = "Name cannot be null")
    @Schema(description = "Name of the dispatch", example = "QC DISPATCH #1")
    private String name;

    @JsonProperty("scheduleType")
    @NotNull(message = "Schedule type cannot be null")
    @Schema(description = "Type of schedule", example = "INTERVAL")
    private ScheduleType scheduleType;

    // Fields for INTERVAL schedule
    @Min(value = 1, message = "Interval minutes must be at least 1")
    @Schema(description = "Interval in minutes for INTERVAL schedule", example = "30")
    private Integer intervalMinutes;

    @Min(value = 1, message = "Repeat count must be at least 1")
    @Schema(description = "Number of times to repeat the dispatch for INTERVAL schedule", example = "5")
    private Integer repeatCount;

    @Schema(description = "Start time for INTERVAL dispatch (ISO-8601 format)", example = "2024-12-17T10:00:00")
    private OffsetDateTime startTime;

    @JsonProperty("timeOfDay")
    // Fields for SPECIFIC_DAYS schedule
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Invalid time format, must be HH:mm")
    @Schema(description = "Time of day for SPECIFIC_DAYS schedule", example = "08:30")
    private String timeOfDay;

    @JsonProperty("specificDays")
    @Schema(description = "List of days for SPECIFIC_DAYS schedule", example = "[\"MONDAY\", \"WEDNESDAY\"]")
    private List<String> specificDays;

    // Common fields
    @JsonProperty("active")
    @NotNull(message = "Active status cannot be null")
    @Schema(description = "Whether the dispatch is active", example = "true")
    private Boolean active;

    @JsonProperty("formIds")
    @NotNull(message = "List of form IDs cannot be empty")
    @Schema(description = "List of form IDs to associate with the dispatch", example = "[101, 102]")
    private List<Long> formIds;

    @JsonProperty("personnelIds")
    @NotNull(message = "List of personnel IDs cannot be empty")
    @Schema(description = "List of personnel IDs to associate with the dispatch", example = "[1001, 1002]")
    private List<Long> personnelIds;
}
