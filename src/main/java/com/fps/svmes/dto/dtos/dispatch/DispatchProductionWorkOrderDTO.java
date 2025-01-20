package com.fps.svmes.dto.dtos.dispatch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DispatchProductionWorkOrderDTO {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("dispatch_id")
    private Long dispatchId;

    @JsonProperty("production_work_order_id")
    private Integer productionWorkOrderId;

    @JsonProperty("status")
    private Short status;
}


