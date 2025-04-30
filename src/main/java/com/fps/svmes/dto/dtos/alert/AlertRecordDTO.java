package com.fps.svmes.dto.dtos.alert;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.dto.dtos.CommonDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(callSuper = true)
public class AlertRecordDTO extends CommonDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("alert_code")
    private String alertCode;

    @JsonProperty("alert_time")
    private OffsetDateTime alertTime;

    @JsonProperty("product_id")
    private Integer productId;

    @JsonProperty("batch_id")
    private Integer batchId;

    @JsonProperty("qc_form_template_id")
    private Long qcFormTemplateId;

    @JsonProperty("inspection_item_key")
    private Long inspectionItemKey;

    @JsonProperty("inspection_value")
    private BigDecimal inspectionValue;

    @JsonProperty("upper_control_limit")
    private BigDecimal upperControlLimit;

    @JsonProperty("lower_control_limit")
    private BigDecimal lowerControlLimit;

    @JsonProperty("rpn")
    private Integer rpn;

    @JsonProperty("risk_level_id")
    private Integer riskLevelId;

    @JsonProperty("inspector_id")
    private Long inspectorId;

    @JsonProperty("reviewer_id")
    private Long reviewerId;

    @JsonProperty("alert_status")
    private Integer alertStatus;
}
