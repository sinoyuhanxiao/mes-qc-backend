package com.fps.svmes.models.sql.formAccessCalendar;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarAssignmentFormId implements Serializable {
    private Long assignment;

    private String formTreeNodeId;
}
