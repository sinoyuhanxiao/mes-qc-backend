package com.fps.svmes.dto.dtos.recipe;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.LinkedHashMap;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ControlLimitSettingDTO {

    @JsonProperty("qc_form_template_id")
    private Long qcFormTemplateId;

    @JsonProperty("control_limits")
    private LinkedHashMap<String, ControlLimitEntry> controlLimits;

    @Data
    public static class ControlLimitEntry {
        @JsonProperty("upper_control_limit")
        private double upperControlLimit;

        @JsonProperty("lower_control_limit")
        private double lowerControlLimit;

        @JsonProperty("label")
        private String label;
    }
}