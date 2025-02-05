package com.fps.svmes.dto.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class InstrumentRequest {
    @JsonProperty("type")
    private String type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("manufacturer")
    private String manufacturer;

    @JsonProperty("modelNumber")
    private String modelNumber;

}
