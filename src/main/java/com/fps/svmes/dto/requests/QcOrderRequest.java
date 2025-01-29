package com.fps.svmes.dto.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QcOrderRequest {

    @JsonProperty("name")
    @NotNull(message = "Name cannot be null")
    @Schema(description = "Name of the order", example = "QC Order Testing Sample")
    private String name;

    private List<DispatchRequest> dispatchRequestList;
}
