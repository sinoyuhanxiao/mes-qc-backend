package com.fps.svmes.dto.dtos.dispatch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.dto.dtos.CommonDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DispatchEquipmentDTO extends CommonDTO {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("dispatch_id")
    private Long dispatchId;

    @JsonProperty("equipment_id")
    private Short equipmentId;
}


