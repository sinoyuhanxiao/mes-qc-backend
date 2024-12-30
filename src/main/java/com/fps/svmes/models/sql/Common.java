package com.fps.svmes.models.sql;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.sql.Timestamp;

@Data
@MappedSuperclass
public abstract class Common {

    @JsonProperty("created_at")
    @Column(name = "created_at")
    private Timestamp createdAt;

    @JsonProperty("created_by")
    @Column(name = "created_by")
    private Integer createdBy;

    @JsonProperty("updated_at")
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @JsonProperty("updated_by")
    @Column(name = "updated_by")
    private Integer updatedBy;

    @JsonProperty("status")
    @Column(name = "status")
    private Integer status;

    public void setCreationDetails(Integer userId, Integer status) {
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.createdBy = userId;
        this.status = status;
    }

    public void setUpdateDetails(Integer userId, Integer status) {
        this.updatedAt = new Timestamp(System.currentTimeMillis());
        this.updatedBy = userId;
        this.status = status;
    }

}
