package com.fps.svmes.models.sql.user;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@Embeddable
public class ShiftUserId implements Serializable {
    private Long shiftId;
    private Long userId;

    public ShiftUserId() {}

    public ShiftUserId(Long shiftId, Long userId) {
        this.shiftId = shiftId;
        this.userId = userId;
    }

    public Long getShiftId() {
        return shiftId;
    }

    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShiftUserId that = (ShiftUserId) o;
        return Objects.equals(shiftId, that.shiftId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shiftId, userId);
    }
}
