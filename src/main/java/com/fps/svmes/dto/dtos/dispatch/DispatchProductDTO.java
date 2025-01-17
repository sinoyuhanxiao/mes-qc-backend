package com.fps.svmes.dto.dtos.dispatch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DispatchProductDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("dispatch_id")
    private Long dispatchId;

    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("status")
    private Integer status;
}

