package com.fps.svmes.dto.dtos.dispatch;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.dto.dtos.user.UserDTO;
import lombok.Data;

@Data
public class DispatchDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("schedule_type")
    private String scheduleType;

    @JsonProperty("time_of_day")
    private String timeOfDay;

    @JsonProperty("interval_minutes")
    private Integer intervalMinutes;

    @JsonProperty("repeat_count")
    private Integer repeatCount;

    @JsonProperty("executed_count")
    private Integer executedCount;

    @JsonProperty("start_time")
    private OffsetDateTime startTime;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt; // Added created_at

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt; // Added updated_at

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("dispatch_days")
    private List<String> dispatchDays;

    @JsonProperty("dispatch_forms")
    private List<Long> formIds;

    @JsonProperty("dispatch_personnel")
    private List<UserDTO> personnel;
}
