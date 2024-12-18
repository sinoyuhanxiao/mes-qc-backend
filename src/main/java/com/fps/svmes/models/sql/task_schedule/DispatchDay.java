package com.fps.svmes.models.sql.task_schedule;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dispatch_day", schema = "quality_management")
@Data
@NoArgsConstructor
public class DispatchDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dispatch_id", nullable = false)
    @JsonBackReference
    private Dispatch dispatch;

    @Column(name = "day", length = 10, nullable = false)
    private String day;

    public DispatchDay(Dispatch dispatch, String specificDay) {
        this.dispatch = dispatch;
        this.day = specificDay;
    }
}


