package com.fps.svmes.dto.dtos.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserDTO {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("role_id")
    private Short roleId;

    @JsonProperty("wecom_id")
    private String wecomId;
}
