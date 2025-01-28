package com.fps.svmes.models.sql.user;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name = "shift_user", schema = "quality_management")
public class ShiftUser {
    @EmbeddedId
    private ShiftUserId id;

    public ShiftUser() {}
    public ShiftUser(Long userId, Long shiftId) {
        this.id = new ShiftUserId(shiftId, userId);
    }

    public ShiftUser(ShiftUserId shiftUserId) {
        this.id = shiftUserId;
    }
}


