package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
import com.fps.svmes.dto.dtos.dispatch.QcOrderDTO;
import com.fps.svmes.models.sql.taskSchedule.*;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchRepository;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchedTaskRepository;
import com.fps.svmes.repositories.jpaRepo.dispatch.QcOrderDispatchRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QcOrderServiceImpl implements QcOrderService {

    @Autowired private QcOrderRepository qcOrderRepo;
    @Autowired private QcOrderDispatchRepository qcOrderDispatchRepo;
    @Autowired private DispatchRepository dispatchRepo;
    @Autowired private DispatchedTaskRepository dispatchedTaskRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private RawMaterialRepository rawMaterialRepository;
    @Autowired private ProductionWorkOrderRepository productionWorkOrderRepository;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private MaintenanceWorkOrderRepository maintenanceWorkOrderRepository;
    @Autowired private DispatchService dispatchService;
    @Autowired private ModelMapper modelMapper;

    //  **Create QC Order**
    @Transactional
    public QcOrderDTO createQcOrder(QcOrderDTO request) {
        QcOrder qcOrder = modelMapper.map(request, QcOrder.class);
        Integer userId = request.getCreatedBy();

        // **Create Dispatches & Map Associations**
        List<Dispatch> dispatches = request.getDispatches().stream()
                .map(dispatchRequest -> {
                    return dispatchService.createDispatch(dispatchRequest);
//                    createDispatchWithAssociations(dispatchRequest, userId);
                })
                .collect(Collectors.toList());

        // **Map to QC Order Dispatch**
        qcOrder.setQcOrderDispatches(mapQcOrderDispatch(qcOrder, dispatches, userId, "CREATE"));

        // **Save QC Order**
        QcOrder savedQcOrder = qcOrderRepo.save(qcOrder);

        return mapQcOrderToDTO(savedQcOrder);
    }

    // ✅ **Update QC Order**
    @Transactional
    public QcOrderDTO updateQcOrder(Long id, QcOrderDTO request) {
        Optional<QcOrder> optionalQcOrder = qcOrderRepo.findById(id);
        Integer userId = request.getUpdatedBy();
        if (optionalQcOrder.isPresent()) {
            QcOrder existingQcOrder = optionalQcOrder.get();

            if (request.getName() != null) {
                existingQcOrder.setName(request.getName());
            }
            if (request.getDescription() != null) {
                existingQcOrder.setDescription(request.getDescription());
            }
            // Handle Dispatches
            if (request.getDispatches() != null) {
                // List of to be updated dispatch from request
                List<Long> incomingDispatchIds = request.getDispatches().stream()
                        .filter(d -> d.getId() != null)
                        .map(DispatchDTO::getId)
                        .toList();

                // Soft delete dispatches no longer in the request
                existingQcOrder.getQcOrderDispatches()
                        .stream()
                        .filter(qcOrderDispatch -> !incomingDispatchIds.contains(qcOrderDispatch.getDispatch().getId()))
                        .forEach(qcOrderDispatch -> {
                            Dispatch dispatch = qcOrderDispatch.getDispatch();
                            dispatch.setStatus(0); // Soft delete by setting status to 0
                            dispatch.setUpdatedBy(userId);
                            dispatch.setUpdatedAt(OffsetDateTime.now());
                            dispatchRepo.save(dispatch); // Save updated dispatch
                        });

                // Process to be updated dispatches
                List<Dispatch> updatedOrCreatedDispatches = request.getDispatches()
                        .stream()
                        .map(dispatchDTO -> {
                            if (dispatchDTO.getId() != null) {
                                // Update existing dispatch
                                Dispatch existingDispatch = dispatchRepo.findById(dispatchDTO.getId())
                                        .orElseThrow(() -> new RuntimeException("Dispatch not found: " + dispatchDTO.getId()));
                                return dispatchService.updateDispatch(existingDispatch.getId(), dispatchDTO);
                            } else {

//                                // Associate new dispatch with the QcOrder
//                                QcOrderDispatch qcOrderDispatch = new QcOrderDispatch();
//                                qcOrderDispatch.setQcOrder(existingQcOrder);
//                                qcOrderDispatch.setDispatch(savedDispatch);
//                                qcOrderDispatchRepo.save(qcOrderDispatch);

                                return dispatchService.createDispatch(dispatchDTO);

                            }
                        })
                        .toList();

                // Update QcOrder's dispatches with updated or newly created ones
                existingQcOrder.setQcOrderDispatches(mapQcOrderDispatch(existingQcOrder, updatedOrCreatedDispatches, userId, "UPDATE"));

            }

            return mapQcOrderToDTO(qcOrderRepo.save(existingQcOrder));
        } else {
            throw new RuntimeException("Order with ID " + id + " not found");
        }
    }

    // ✅ **Get QC Order by ID**
    @Transactional(readOnly = true)
    public QcOrderDTO getQcOrderById(Long id) {
        Optional<QcOrder> qcOrder = qcOrderRepo.findByIdAndStatus(id, (short)1);
        if (qcOrder.isPresent()){
            return mapQcOrderToDTO(qcOrder.get());
        } else {
            throw new EntityNotFoundException("QC Order with ID " + id + " not found");
        }
    }

    // ✅ **Get All QC Orders**
    @Transactional(readOnly = true)
    public List<QcOrderDTO> getAllQcOrders() {
        return qcOrderRepo.findAll()
                .stream()
                .filter(qcOrder -> qcOrder.getStatus() == 1)
                .map(this::mapQcOrderToDTO).collect(Collectors.toList());
    }

    // ✅ **Delete QC Order**
    @Transactional
    public void deleteQcOrder(Long orderId, Integer userId) {
        QcOrder qcOrder = qcOrderRepo.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("QC Order not found"));

        // ** Soft-delete order & associated dispatches**
        qcOrder.setStatus(0);
        qcOrder.getQcOrderDispatches().forEach(qcOrderDispatch -> {
            dispatchService.deleteDispatch(qcOrderDispatch.getDispatch().getId(), userId);
            qcOrderDispatch.setStatus(0);
        });

        qcOrderRepo.save(qcOrder);
    }

    // ✅ **Helper: Map QC Order to Dispatches**
    private List<QcOrderDispatch> mapQcOrderDispatch(QcOrder qcOrder, List<Dispatch> dispatches, Integer userId, String action) {
        if (action.equals("CREATE")) {
            return dispatches.stream()
                    .map(dispatch -> {
                        QcOrderDispatch qcOrderDispatch = new QcOrderDispatch(qcOrder, dispatch);
                        qcOrderDispatch.setCreationDetails(userId, 1);
                        return qcOrderDispatch;
                    })
                    .toList();
        } else {
            return dispatches.stream()
                    .map(dispatch -> {
                        QcOrderDispatch qcOrderDispatch = new QcOrderDispatch(qcOrder, dispatch);
                        qcOrderDispatch.setUpdateDetails(userId, 1);
                        return qcOrderDispatch;
                    })
                    .toList();
        }
    }

    // ✅ **Helper: Convert QcOrder to QcOrderDTO**
    private QcOrderDTO mapQcOrderToDTO(QcOrder qcOrder) {
        QcOrderDTO qcOrderDTO = modelMapper.map(qcOrder, QcOrderDTO.class);

        List<DispatchDTO> dispatchDTOs = qcOrder.getQcOrderDispatches().stream()
                .map(qcOrderDispatch ->
                        dispatchService.convertToDispatchDTO(qcOrderDispatch.getDispatch()))
                .toList();

        qcOrderDTO.setDispatches(dispatchDTOs);
        return qcOrderDTO;
    }
}
