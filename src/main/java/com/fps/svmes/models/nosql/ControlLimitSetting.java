package com.fps.svmes.models.nosql;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

@Data
@Document(collection = "control_limit_setting")
public class ControlLimitSetting {

    @Id
    private String id;

    @Field("qc_form_template_id")
    private Long qcFormTemplateId;

    @Field("control_limits")
    private Map<String, ControlLimitEntry> controlLimits;

    @Data
    public static class ControlLimitEntry {
        @Field("upper_control_limit")
        private Double upperControlLimit;

        @Field("lower_control_limit")
        private Double lowerControlLimit;

        @Field("label")
        private String label;
    }
}
