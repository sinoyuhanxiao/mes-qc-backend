package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
import com.fps.svmes.dto.dtos.dispatch.QcOrderDTO;
import com.fps.svmes.dto.requests.DispatchRequest;
import com.fps.svmes.dto.requests.QcOrderRequest;
import com.fps.svmes.models.sql.taskSchedule.*;
import com.fps.svmes.models.sql.user.User;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchRepository;
import com.fps.svmes.repositories.jpaRepo.dispatch.QcOrderRepository;
import com.fps.svmes.repositories.jpaRepo.maintenance.EquipmentRepository;
import com.fps.svmes.repositories.jpaRepo.maintenance.MaintenanceWorkOrderRepository;
import com.fps.svmes.repositories.jpaRepo.production.ProductRepository;
import com.fps.svmes.repositories.jpaRepo.production.ProductionWorkOrderRepository;
import com.fps.svmes.repositories.jpaRepo.production.RawMaterialRepository;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import com.fps.svmes.services.DispatchService;
import com.fps.svmes.services.QcOrderService;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Service
public class QcOrderServiceImpl implements QcOrderService {

    @Autowired private QcOrderRepository qcOrderRepo;
    @Autowired private DispatchRepository dispatchRepo;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private RawMaterialRepository rawMaterialRepository;
    @Autowired private ProductionWorkOrderRepository productionWorkOrderRepository;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private MaintenanceWorkOrderRepository maintenanceWorkOrderRepository;
    @Autowired private DispatchService dispatchService;
    @Autowired private ModelMapper modelMapper;

    // ✅ **Create QC Order**
    @Transactional
    public QcOrderDTO createQcOrder(QcOrderRequest request, Integer userId) {
        QcOrder qcOrder = new QcOrder();
        qcOrder.setName(request.getName());
        qcOrder.setCreationDetails(userId, 1);

        // **Create Dispatches & Map Associations**
        List<Dispatch> dispatches = request.getDispatchRequestList().stream()
                .map(dispatchRequest -> createDispatchWithAssociations(dispatchRequest, userId))
                .collect(Collectors.toList());

        // **Map to QC Order Dispatch**
        qcOrder.setQcOrderDispatches(mapQcOrderDispatch(qcOrder, dispatches, userId));

        // **Save QC Order**
        QcOrder savedQcOrder = qcOrderRepo.save(qcOrder);

        // **Initialize Dispatch Scheduling**
        savedQcOrder.getQcOrderDispatches().forEach(qcOrderDispatch ->
                dispatchService.initializeDispatch(
                        qcOrderDispatch.getDispatch().getId(),
                        () -> dispatchService.executeDispatch(qcOrderDispatch.getDispatch().getId())
                )
        );

        return mapQcOrderToDTO(savedQcOrder);
    }

    // ✅ **Update QC Order**
    @Transactional
    public QcOrderDTO updateQcOrder(Long id, QcOrderRequest request, Integer userId) {
        QcOrder qcOrder = qcOrderRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("QC Order not found"));
//
//        qcOrder.setName(request.getName());
//
//        // **Soft-delete existing dispatches & remove tasks**
//        qcOrder.getQcOrderDispatches().forEach(qcOrderDispatch -> {
//            dispatchService.deleteDispatch(qcOrderDispatch.getDispatch().getId());
//            qcOrderDispatch.setStatus(0);
//        });
//        qcOrder.getQcOrderDispatches().clear();
//
//        // **Create updated Dispatches**
//        List<Dispatch> updatedDispatches = request.getDispatchRequestList().stream()
//                .map(dispatchRequest -> {
//                    Dispatch dispatch = modelMapper.map(dispatchRequest, Dispatch.class);
//                    dispatch.setCreationDetails(userId, 1);
//                    dispatchRepo.save(dispatch);
//                    mapDispatchAssociations(dispatch, dispatchRequest, userId);
//                    return dispatch;
//                })
//                .collect(Collectors.toList());
//
//        // **Update QC Order Dispatch Mappings**
//        qcOrder.setQcOrderDispatches(mapQcOrderDispatch(qcOrder, updatedDispatches, userId));
//
//        return mapQcOrderToDTO(qcOrderRepo.save(qcOrder));
        return new QcOrderDTO();
    }

    // ✅ **Get QC Order by ID**
    @Transactional(readOnly = true)
    public QcOrderDTO getQcOrderById(Long orderId) {
        return qcOrderRepo.findById(orderId)
                .map(this::mapQcOrderToDTO)
                .orElseThrow(() -> new EntityNotFoundException("QC Order not found"));
    }

    // ✅ **Get All QC Orders**
    @Transactional(readOnly = true)
    public List<QcOrderDTO> getAllQcOrders() {
        return qcOrderRepo.findAll().stream().map(this::mapQcOrderToDTO).collect(Collectors.toList());
    }

    // ✅ **Delete QC Order**
    @Transactional
    public void deleteQcOrder(Long orderId, Integer userId) {
        QcOrder qcOrder = qcOrderRepo.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("QC Order not found"));

        // **Soft-delete order & associated dispatches**
        qcOrder.setStatus(0);
        qcOrder.getQcOrderDispatches().forEach(qcOrderDispatch -> {
            dispatchService.deleteDispatch(qcOrderDispatch.getDispatch().getId());
            qcOrderDispatch.setStatus(0);
        });

        qcOrderRepo.save(qcOrder);
    }

    // ✅ **Pause Dispatch**
    @Transactional
    public void pauseDispatch(Long orderId, Long dispatchId, Integer userId) {
        dispatchService.pauseDispatch(dispatchId, userId);
    }

    // ✅ **Resume Dispatch**
    @Transactional
    public void resumeDispatch(Long orderId, Long dispatchId, Integer userId) {
        dispatchService.resumeDispatch(dispatchId, userId);
    }

    // ✅ **Helper: Create Dispatch with Mappings**
    private Dispatch createDispatchWithAssociations(DispatchRequest request, Integer userId) {
        Dispatch dispatch = modelMapper.map(request, Dispatch.class);
        dispatch.setCreationDetails(userId, 1);
        Dispatch savedDispatch = dispatchRepo.save(dispatch); // Ensure dispatch is persisted first

        // Map associations
        savedDispatch.setDispatchForms(mapDispatchForms(savedDispatch, request.getFormIds()));
        savedDispatch.setDispatchUsers(mapEntities(savedDispatch, request.getUserIds(), userRepository, "User", DispatchUser::new));
        savedDispatch.setDispatchProducts(mapEntities(savedDispatch, request.getProductIds(), productRepository, "Product", DispatchProduct::new));
        savedDispatch.setDispatchRawMaterials(mapEntities(savedDispatch, request.getRawMaterialIds(), rawMaterialRepository, "Raw Material", DispatchRawMaterial::new));
        savedDispatch.setDispatchProductionWorkOrders(mapEntities(savedDispatch, request.getProductionWorkOrderIds(), productionWorkOrderRepository, "Production Work Order", DispatchProductionWorkOrder::new));
        savedDispatch.setDispatchEquipments(mapEntities(savedDispatch, request.getEquipmentIds(), equipmentRepository, "Equipment", DispatchEquipment::new));
        savedDispatch.setDispatchMaintenanceWorkOrders(mapEntities(savedDispatch, request.getMaintenanceWorkOrderIds(), maintenanceWorkOrderRepository, "Maintenance Work Order", DispatchMaintenanceWorkOrder::new));

        return dispatchRepo.save(savedDispatch); // Save again after setting associations
    }

    // ✅ **Generic Mapper for Entity Fetching**
    private <ID, E, R> List<R> mapEntities(Dispatch dispatch, List<ID> ids, JpaRepository<E, ID> repository, String entityName, BiFunction<Dispatch, E, R> mapper) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream()
                .map(id -> {
                    E entity = repository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException(entityName + " not found: " + id));
                    return mapper.apply(dispatch, entity);
                })
                .toList();
    }

    // ✅ **Helper: Map QC Order to Dispatches**
    private List<QcOrderDispatch> mapQcOrderDispatch(QcOrder qcOrder, List<Dispatch> dispatches, Integer userId) {
        return dispatches.stream()
                .map(dispatch -> {
                    QcOrderDispatch qcOrderDispatch = new QcOrderDispatch(qcOrder, dispatch);
                    qcOrderDispatch.setCreationDetails(userId, 1);
                    return qcOrderDispatch;
                })
                .toList();
    }

    // ✅ **Helper: Convert QcOrder to QcOrderDTO**
    private QcOrderDTO mapQcOrderToDTO(QcOrder qcOrder) {
        QcOrderDTO qcOrderDTO = modelMapper.map(qcOrder, QcOrderDTO.class);

        List<DispatchDTO> dispatchDTOs = qcOrder.getQcOrderDispatches().stream()
                .map(qcOrderDispatch -> modelMapper.map(qcOrderDispatch.getDispatch(), DispatchDTO.class))
                .toList();

        qcOrderDTO.setDispatches(dispatchDTOs);
        return qcOrderDTO;
    }

    private List<DispatchForm> mapDispatchForms(Dispatch dispatch, List<String> formIds) {
        return formIds.stream()
                .map(formTreeNodeId -> new DispatchForm(dispatch, formTreeNodeId))
                .toList();
    }
}
