package com.fps.svmes.models.sql.user;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shift_form", schema = "quality_management")
public class ShiftForm {
    @EmbeddedId
    ShiftFormId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("shiftId")
    @JoinColumn(name = "shift_id", referencedColumnName = "id")
    private Shift shift;
}