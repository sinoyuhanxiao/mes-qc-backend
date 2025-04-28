package com.fps.svmes.models.sql.formAccessCalendar;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "calendar_assignment_form", schema = "quality_management")
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class CalendarAssignmentForm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    @JoinColumn(name = "assignment_id", nullable = false)
    private CalendarAssignment assignment;

    @Column(name = "form_tree_node_id", nullable = false)
    private String formTreeNodeId;
}

