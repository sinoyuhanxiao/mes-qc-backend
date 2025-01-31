package com.fps.svmes.models.sql.taskSchedule;

import com.fps.svmes.models.sql.Common;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "dispatch_instrument", schema = "quality_management")
@Data
@NoArgsConstructor
public class DispatchInstrument extends Common {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dispatch_id")
    private Dispatch dispatch;

    @Column(name = "instrument_id")
    private Long instrumentId;
}
