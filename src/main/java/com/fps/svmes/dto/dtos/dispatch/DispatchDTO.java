package com.fps.svmes.dto.dtos.dispatch;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.dto.dtos.production.ProductDTO;
import com.fps.svmes.dto.dtos.user.UserDTO;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class DispatchDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("remark")
    private String remark;

    @JsonProperty("cron_expression")
    private String cronExpression;

    @JsonProperty("start_time")
    private OffsetDateTime startTime;

    @JsonProperty("end_time")
    private OffsetDateTime endTime;

    @JsonProperty("dispatch_limit")
    private Integer dispatchLimit;

    @JsonProperty("executed_count")
    private Integer executedCount;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("created_by")
    private Integer createdBy;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    @JsonProperty("updated_by")
    private Integer updatedBy;

    @JsonProperty("due_date_offset_minute")
    private Integer dueDateOffsetMinute; // Total offset in minutes

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("dispatch_forms")
    private List<String> qcFormTreeNodeIds;

    @JsonProperty("dispatch_users")
    private List<UserDTO> users;

    @JsonProperty("dispatch_products")
    private List<DispatchProductDTO> products;

    @JsonProperty("dispatch_raw_materials")
    private List<DispatchRawMaterialDTO> rawMaterials;

    @JsonProperty("dispatch_production_work_orders")
    private List<DispatchProductionWorkOrderDTO> productionWorkOrders;

    @JsonProperty("dispatch_equipments")
    private List<DispatchEquipmentDTO> equipments;

    @JsonProperty("dispatch_maintenance_work_orders")
    private List<DispatchMaintenanceWorkOrderDTO> maintenanceWorkOrders;
}
