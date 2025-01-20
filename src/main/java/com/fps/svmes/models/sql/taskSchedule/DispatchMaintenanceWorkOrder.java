package com.fps.svmes.models.sql.taskSchedule;

import com.fps.svmes.models.sql.maintenance.MaintenanceWorkOrder;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dispatch_maintenance_work_order", schema = "quality_management")
@Data
@NoArgsConstructor
public class DispatchMaintenanceWorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_id", nullable = false)
    private Dispatch dispatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_work_order_id", nullable = false)
    private MaintenanceWorkOrder maintenanceWorkOrder;

    @Column(name = "status", nullable = false, columnDefinition = "SMALLINT DEFAULT 1")
    private Short status = 1; // Default active

    public DispatchMaintenanceWorkOrder(Dispatch dispatch, MaintenanceWorkOrder maintenanceWorkOrder) {
        this.dispatch = dispatch;
        this.maintenanceWorkOrder = maintenanceWorkOrder;
    }
}




