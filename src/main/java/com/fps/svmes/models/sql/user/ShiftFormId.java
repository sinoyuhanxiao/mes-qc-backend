package com.fps.svmes.models.sql.user;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ShiftFormId implements Serializable {
    private Integer shiftId;
    private String formId; // String to match MongoDB's FormNode._id
}