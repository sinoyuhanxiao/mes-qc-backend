package com.fps.svmes.dto.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

@Data
@MappedSuperclass
public abstract class CommonDTO {
    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("created_by")
    private Integer createdBy;

    @JsonProperty("updated_at")
    private Long updatedAt;

    @JsonProperty("updated_by")
    private Integer updatedBy;

    @JsonProperty("status")
    private Integer status;
}
