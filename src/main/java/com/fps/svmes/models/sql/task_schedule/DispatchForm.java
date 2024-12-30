package com.fps.svmes.models.sql.task_schedule;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dispatch_form", schema = "quality_management")
@Data
@NoArgsConstructor
public class DispatchForm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dispatch_id", nullable = false)
    @JsonBackReference
    private Dispatch dispatch;

    @Column(name = "form_id", nullable = false)
    private String formId;



    public DispatchForm( Dispatch dispatch, String formId) {
        this.dispatch = dispatch;
        this.formId = formId;
    }
}
