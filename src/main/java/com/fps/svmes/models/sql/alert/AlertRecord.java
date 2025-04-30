package com.fps.svmes.models.sql.alert;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.models.sql.Common;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "qc_alert_record", schema = "quality_management")
@Data
@EqualsAndHashCode(callSuper = true)
public class AlertRecord extends Common {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @JsonProperty("id")
    private Long id;

    @Column(name = "alert_code")
    @JsonProperty("alert_code")
    private String alertCode;

    @Column(name = "alert_time")
    @JsonProperty("alert_time")
    private OffsetDateTime alertTime;

    @Column(name = "product_id")
    @JsonProperty("product_id")
    private Integer productId;

    @Column(name = "batch_id")
    @JsonProperty("batch_id")
    private Integer batchId;

    @Column(name = "qc_form_template_id")
    @JsonProperty("qc_form_template_id")
    private Long qcFormTemplateId;

    @Column(name = "inspection_item_key")
    @JsonProperty("inspection_item_key")
    private Long inspectionItemKey;

    @Column(name = "inspection_value")
    @JsonProperty("inspection_value")
    private BigDecimal inspectionValue;

    @Column(name = "upper_control_limit")
    @JsonProperty("upper_control_limit")
    private BigDecimal upperControlLimit;

    @Column(name = "lower_control_limit")
    @JsonProperty("lower_control_limit")
    private BigDecimal lowerControlLimit;

    @Column(name = "rpn")
    @JsonProperty("rpn")
    private Integer rpn;

    @Column(name = "risk_level_id")
    @JsonProperty("risk_level_id")
    private Integer riskLevelId;

    @Column(name = "inspector_id")
    @JsonProperty("inspector_id")
    private Long inspectorId;

    @Column(name = "reviewer_id")
    @JsonProperty("reviewer_id")
    private Long reviewerId;

    @Column(name = "alert_status")
    @JsonProperty("alert_status")
    private Integer alertStatus;
}
