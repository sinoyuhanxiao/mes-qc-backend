package com.fps.svmes.dto.dtos.dispatch;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DispatchedTaskDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("dispatch_id")
    private Long dispatchId;

    @JsonProperty("personnel_id")
    private Long personnelId;

    @JsonProperty("form_id")
    private String formId;

    @JsonProperty("dispatch_time")
    private OffsetDateTime dispatchTime;

    @JsonProperty("status")
    private String status;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("updated_at")
    private OffsetDateTime  updatedAt;
}
