package com.fps.svmes.dto.dtos.dispatch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DispatchProductionWorkOrderDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("dispatch_id")
    private Long dispatchId;

    @JsonProperty("production_work_order_id")
    private Long productionWorkOrderId;

    @JsonProperty("status")
    private Integer status;
}


