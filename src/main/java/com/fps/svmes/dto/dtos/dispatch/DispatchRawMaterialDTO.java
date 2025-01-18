package com.fps.svmes.dto.dtos.dispatch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DispatchRawMaterialDTO {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("dispatch_id")
    private Long dispatchId;

    @JsonProperty("raw_material_id")
    private Integer rawMaterialId;

    @JsonProperty("status")
    private Short status;
}


