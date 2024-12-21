package com.fps.svmes.dto.dtos.dispatch;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DispatchedTaskDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("dispatch_id")
    private Long dispatchId;

    @JsonProperty("personnel_id")
    private Long personnelId;

    @JsonProperty("form_id")
    private Long formId;

    @JsonProperty("dispatch_time")
    private LocalDateTime dispatchTime;

    @JsonProperty("status")
    private String status;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("last_updated")
    private LocalDateTime lastUpdated;

    @JsonProperty("finished_at")
    private LocalDateTime finishedAt;
}
