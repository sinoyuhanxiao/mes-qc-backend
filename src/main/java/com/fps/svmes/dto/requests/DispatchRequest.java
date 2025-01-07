package com.fps.svmes.dto.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DispatchRequest {

    @JsonProperty("name")
    @NotNull(message = "Name cannot be null")
    @Schema(description = "Name of the dispatch", example = "QC Dispatch Sample")
    private String name;

    @JsonProperty("type")
    @NotNull(message = "Type cannot be null")
    @Schema(description = "Type of the dispatch", example = "SCHEDULED or MANUAL")
    private String type;

    @JsonProperty("remark")
    @Schema(description = "Remark or note for the dispatch", example = "This is a high-priority dispatch.")
    private String remark;

    @JsonProperty("cronExpression")
    @Schema(description = "Cron expression defining the schedule", example = "0 * * * * *")
    private String cronExpression;

    @JsonProperty("startTime")
    @NotNull(message = "Start time cannot be null")
    @Schema(description = "Start time of the dispatch (ISO 8601 format)", example = "2025-01-01T08:00:00Z")
    private OffsetDateTime startTime;

    @JsonProperty("endTime")
    @NotNull(message = "End time cannot be null")
    @Schema(description = "End time of the dispatch (ISO 8601 format)", example = "2025-12-31T18:00:00Z")
    private OffsetDateTime endTime;

    @JsonProperty("dispatchLimit")
    @Schema(description = "Maximum number of times the dispatch can be executed", example = "10")
    private Integer dispatchLimit;

    @JsonProperty("dueDateOffsetMinute")
    @NotNull(message = "Due date offset in minutes cannot be null")
    @Schema(description = "Offset in minutes to calculate the due date of dispatched tasks", example = "1440")
    private Integer dueDateOffsetMinute;

    @JsonProperty("formIds")
    @NotNull(message = "List of form IDs cannot be null")
    @Schema(description = "List of form IDs associated with the dispatch", example = "[\"form1\", \"form2\"]")
    private List<String> formIds;

    @JsonProperty("userIds")
    @NotNull(message = "List of user IDs cannot be null")
    @Schema(description = "List of user IDs associated with the dispatch", example = "[1001, 1002]")
    private List<Integer> userIds;

    @JsonProperty("created_by")
    @Schema(description = "User ID that creates this dispatch", example = "14")
    private Integer createdBy;

    @JsonProperty("updated_by")
    @Schema(description = "User ID that updates this dispatch", example = "null")
    private Integer updatedBy;

}
