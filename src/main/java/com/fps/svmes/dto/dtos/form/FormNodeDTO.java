package com.fps.svmes.dto.dtos.form;

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

    @JsonProperty("children")
    private List<FormNodeDTO> children;

}