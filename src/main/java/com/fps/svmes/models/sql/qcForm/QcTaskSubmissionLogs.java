package com.fps.svmes.models.sql.qcForm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.converters.JsonNodeConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fps.svmes.models.sql.Common;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Entity
@Table(name = "qc_task_submission_logs", schema = "quality_management")
@Data
public class QcTaskSubmissionLogs extends Common{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "qc_task_submission_logs_seq")
    @SequenceGenerator(name = "qc_task_submission_logs_seq", sequenceName = "quality_management.qc_task_submission_logs_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "submission_id", length = 255, nullable = false)
    private String submissionId;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private Integer reviewedBy;

    @Column(name = "comment")
    private String comment;

    @Column(name = "dispatched_task_id")
    private Long dispatchedTaskId;

    @JsonProperty("qc_form_template_id")
    private Long qcFormTemplateId;

    // Getters and Setters
}
