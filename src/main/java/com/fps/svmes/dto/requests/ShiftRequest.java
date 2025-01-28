package com.fps.svmes.dto.requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetTime;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShiftRequest {

    @JsonProperty("id")
    @Schema(description = "Shift ID (only for updates)", example = "1")
    private Integer id;

    @JsonProperty("name")
    @NotNull(message = "Shift name cannot be null")
    @Schema(description = "Name of the shift", example = "Morning Shift")
    private String name;

    @JsonProperty("type")
    @NotNull(message = "Shift type cannot be null")
    @Schema(description = "Type of the shift", example = "Day")
    private String type;

    @JsonProperty("leader_id") // 与前端字段名称一致
    @Schema(description = "User ID of the shift leader", example = "14")
    private Integer leaderId;

    @Schema(type = "string", pattern = "HH:mm:ssXXX", example = "08:00:00+02:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ssXXX")
    @JsonProperty("start_time")
    private OffsetTime startTime;

    @Schema(type = "string", pattern = "HH:mm:ssXXX", example = "16:00:00+02:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ssXXX")
    @JsonProperty("end_time")
    private OffsetTime endTime;

    @JsonProperty("description")
    @Schema(description = "Description of the shift", example = "This is a regular day shift")
    private String description;

    @JsonProperty("status")
    @Schema(description = "Status of the shift", example = "1")
    private Integer status;

    @JsonProperty("created_by")
    @Schema(description = "User ID that creates this shift", example = "14")
    private Integer createdBy;

    @JsonProperty("updated_by")
    @Schema(description = "User ID that updates this shift", example = "14")
    private Integer updatedBy;

    @JsonProperty("created_at")
    @Schema(description = "Creation timestamp of the shift", example = "12:00:00+00:00")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    @Schema(description = "Last update timestamp of the shift", example = "12:00:00+00:00")
    private OffsetDateTime updatedAt;
}
