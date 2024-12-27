package com.fps.svmes.dto.dtos.qcForm;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.List;

@Data
public class FormNodeDTO {

    @Id
    @JsonProperty("_id")
    private String id;

    @JsonProperty("label")
    private String label;

    @JsonProperty("node_type")
    private String nodeType;

    @JsonProperty("qc_form_template_id")
    private Long qcFormTemplateId;

    @JsonProperty("children")
    private List<FormNodeDTO> children;

}