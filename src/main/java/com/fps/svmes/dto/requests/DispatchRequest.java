package com.fps.svmes.dto.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("type")
    @NotNull(message = "Type cannot be null")
    @Schema(description = "Type of the dispatch", example = "SCHEDULED or MANUAL")
    private String type;

    @JsonProperty("name")
    @Schema(description = "Name of the dispatch", example = "QC Dispatch Testing Sample")
    private String name;

    @JsonProperty("description")
    @Schema(description = "Description for the dispatch", example = "This is a test dispatch.")
    private String description;

    @JsonProperty("startTime")
    @Schema(description = "Start time of the dispatch (ISO 8601 format)", example = "2025-01-01T08:00:00Z")
    private OffsetDateTime startTime;

    @JsonProperty("endTime")
    @Schema(description = "End time of the dispatch (ISO 8601 format)", example = "2025-12-31T18:00:00Z")
    private OffsetDateTime endTime;

    @JsonProperty("cronExpression")
    @Schema(description = "Cron expression defining the schedule", example = "0 * * * * *")
    private String cronExpression;

    @JsonProperty("dispatchLimit")
    @Schema(description = "Maximum number of times the dispatch can be executed", example = "10")
    private Integer dispatchLimit;

    @JsonProperty("customTime")
    @Schema(description = "Custom time for custom type dispatch")
    private OffsetDateTime customTime;

    @JsonProperty("dueDateOffsetMinute")
    @NotNull(message = "Due date offset in minutes cannot be null")
    @Schema(description = "Offset in minutes to calculate the due date of dispatched tasks", example = "60")
    private Integer dueDateOffsetMinute;

    @JsonProperty("userIds")
    @NotNull(message = "List of user IDs cannot be null")
    @Schema(description = "List of user IDs associated with the dispatch", example = "[14]")
    private List<Integer> userIds;

    @JsonProperty("formIds")
    @NotNull(message = "List of form IDs cannot be null")
    @Schema(description = "List of form IDs associated with the dispatch", example = "[\"form1\", \"form2\"]")
    private List<String> formIds;

    @JsonProperty("productIds")
    @Schema(description = "List of product IDs associated with the dispatch", example = "[1, 2, 3]")
    private List<Integer> productIds;

    @JsonProperty("rawMaterialIds")
    @Schema(description = "List of raw Material IDs associated with the dispatch", example = "[1, 2, 3]")
    private List<Integer> rawMaterialIds;

    @JsonProperty("productionWorkOrderIds")
    @Schema(description = "List of production Work Order IDs associated with the dispatch", example = "[1, 2, 3]")
    private List<Integer> productionWorkOrderIds;

    @JsonProperty("equipmentIds")
    @Schema(description = "List of equipment IDs associated with the dispatch", example = "[1, 2, 3]")
    private List<Short> equipmentIds;

    @JsonProperty("maintenanceWorkOrderIds")
    @Schema(description = "List of maintenance Work Order IDs associated with the dispatch", example = "[1, 2, 3]")
    private List<Integer> maintenanceWorkOrderIds;

    @JsonProperty("samplingLocationIds")
    @Schema(description = "List of sampling location IDs associated with the dispatch", example = "[1, 2, 3]")
    private List<Long> samplingLocationIds;

    @JsonProperty("instrumentIds")
    @Schema(description = "List of instrument IDs associated with the dispatch", example = "[1, 2, 3]")
    private List<Long> instrumentIds;

    @JsonProperty("testSubjectIds")
    @Schema(description = "List of test subject IDs associated with the dispatch", example = "[1, 2, 3]")
    private List<Long> testSubjectIds;

}
