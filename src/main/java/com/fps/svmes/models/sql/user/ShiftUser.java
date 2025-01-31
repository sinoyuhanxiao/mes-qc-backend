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

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("shiftId") // This tells Hibernate to use the shiftId from the composite key
    @JoinColumn(name = "shift_id", referencedColumnName = "id")
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId") // This tells Hibernate to use the userId from the composite key
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    public ShiftUser() {}

    public ShiftUser(Integer userId, Integer shiftId) {
        this.id = new ShiftUserId(shiftId, userId);
    }

    public ShiftUser(ShiftUserId shiftUserId) {
        this.id = shiftUserId;
    }
}


