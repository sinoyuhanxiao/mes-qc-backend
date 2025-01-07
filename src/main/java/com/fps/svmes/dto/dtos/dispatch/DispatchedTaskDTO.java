package com.fps.svmes.dto.dtos.dispatch;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.dto.dtos.user.UserDTO;
import com.fps.svmes.models.sql.user.User;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class DispatchedTaskDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("dispatch_id")
    private Long dispatchId;

    @JsonProperty("user")
    private UserDTO user;

    @JsonProperty("qc_form_tree_node_id")
    private String qcFormTreeNodeId;

    @JsonProperty("dispatch_time")
    private Timestamp dispatchTime;

    @JsonProperty("state")
    private String state;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("finished_at")
    private Timestamp finishedAt;

    @JsonProperty("updated_at")
    private Timestamp updatedAt;

    @JsonProperty("created_by")
    private Integer created_by;

    @JsonProperty("updated_by")
    private Integer updated_by;

    @JsonProperty("status")
    private Integer status;

}
