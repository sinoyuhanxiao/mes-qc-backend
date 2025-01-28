package com.fps.svmes.dto.dtos.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShiftUserDTO {

    @JsonProperty("shift_id")
    private Long shiftId;

    @JsonProperty("user_id")
    private Long userId;

    public ShiftUserDTO(Long userId, Long shiftId) {
        this.userId = userId;
        this.shiftId = shiftId;
    }
}
