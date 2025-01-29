package com.fps.svmes.dto.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.dto.dtos.dispatch.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DispatchRequest {

    @JsonProperty("name")
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
    @Schema(description = "Start time of the dispatch (ISO 8601 format)", example = "2025-01-01T08:00:00Z")
    private OffsetDateTime startTime;

    @JsonProperty("endTime")
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

    @JsonProperty("productIds")
    @Schema(description = "List of Product IDs associated with the dispatch", example = "[1, 2, 3]")
    private List<Integer> productIds;

    @JsonProperty("rawMaterialIds")
    @Schema(description = "List of Raw Material IDs associated with the dispatch", example = "[1, 2, 3]")
    private List<Integer> rawMaterialIds;

    @JsonProperty("productionWorkOrderIds")
    @Schema(description = "List of Production Work Order IDs associated with the dispatch", example = "[1, 2, 3]")
    private List<Integer> productionWorkOrderIds;

    @JsonProperty("equipmentIds")
    @Schema(description = "List of Equipment IDs associated with the dispatch", example = "[1, 2, 3]")
    private List<Short> equipmentIds;

    @JsonProperty("maintenanceWorkOrderIds")
    @Schema(description = "List of Maintenance Work Order IDs associated with the dispatch", example = "[1, 2, 3]")
    private List<Integer> maintenanceWorkOrderIds;
}
