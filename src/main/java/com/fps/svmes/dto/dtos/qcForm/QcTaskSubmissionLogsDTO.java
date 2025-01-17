package com.fps.svmes.dto.dtos.qcForm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.dto.dtos.CommonDTO;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QcTaskSubmissionLogsDTO extends CommonDTO {
    @Id
    @JsonProperty("id")
    private Long id;

    @JsonProperty("submission_id")
    private String submissionId;

    @JsonProperty("reviewed_at")
    private String reviewedAt;

    @JsonProperty("reviewed_by")
    private Integer reviewedBy;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("dispatched_task_id")
    private Long dispatchedTaskId;

    @JsonProperty("qc_form_template_id")
    private Long qcFormTemplateId;
}
