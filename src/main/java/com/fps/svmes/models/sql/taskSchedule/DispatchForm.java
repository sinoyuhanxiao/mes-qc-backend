package com.fps.svmes.models.sql.taskSchedule;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dispatch_form_temp", schema = "quality_management")
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

    @Column(name = "qc_form_tree_node_id", nullable = false)
    private String qcFormTreeNodeId;

    @Column(name = "status", nullable = false)
    private Short status = 1; // Active by default

    public DispatchForm(Dispatch dispatch, String formTreeNodeId) {
        this.setQcFormTreeNodeId(formTreeNodeId);
        this.dispatch = dispatch;
    }
}
