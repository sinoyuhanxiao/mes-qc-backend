package com.fps.svmes.models.sql.task_schedule;

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
    private Dispatch dispatch;

    @Column(name = "day", length = 10, nullable = false)
    private String day;

    public DispatchDay(Long id, Dispatch dispatch, String specificDay) {
        this.id = id;
        this.dispatch = dispatch;
        this.day = specificDay;
    }

    public DispatchDay(Dispatch dispatch, String specificDay) {
        this.dispatch = dispatch;
        this.day = specificDay;
    }
}


