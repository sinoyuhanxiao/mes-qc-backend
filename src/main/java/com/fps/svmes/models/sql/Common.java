package com.fps.svmes.models.sql;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.sql.Timestamp;
import java.time.OffsetDateTime;

@Data
@MappedSuperclass
public abstract class Common {

    @JsonProperty("created_at")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("created_by")
    @Column(name = "created_by")
    private Integer createdBy;

    @JsonProperty("updated_at")
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @JsonProperty("updated_by")
    @Column(name = "updated_by")
    private Integer updatedBy;

    @JsonProperty("status")
    @Column(name = "status", nullable = false, columnDefinition = "SMALLINT DEFAULT 1")
    private Integer status;

    public void setDetails(Integer userId, Integer status) {
        this.createdAt = OffsetDateTime.now();
        this.createdBy = userId;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = userId;
        this.status = status;
    }

}
