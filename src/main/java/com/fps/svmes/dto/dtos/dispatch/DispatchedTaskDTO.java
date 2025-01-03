package com.fps.svmes.dto.dtos.dispatch;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.dto.dtos.user.UserDTO;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DispatchedTaskDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("dispatch_id")
    private Long dispatchId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("qc_form_tree_node_id")
    private String qcFormTreeNodeId;

    @JsonProperty("dispatch_time")
    private OffsetDateTime dispatchTime;

    @JsonProperty("state")
    private String state;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("finished_at")
    private OffsetDateTime finishedAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    @JsonProperty("created_by")
    private Integer created_by;

    @JsonProperty("updated_by")
    private Integer updated_by;

    @JsonProperty("status")
    private Integer status;

}
