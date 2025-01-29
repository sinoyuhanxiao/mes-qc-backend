package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
import com.fps.svmes.dto.dtos.dispatch.QcOrderDTO;
import com.fps.svmes.dto.requests.QcOrderRequest;
import com.fps.svmes.models.sql.taskSchedule.Dispatch;
import com.fps.svmes.models.sql.taskSchedule.QcOrder;
import com.fps.svmes.models.sql.taskSchedule.QcOrderDispatch;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchRepository;
import com.fps.svmes.repositories.jpaRepo.dispatch.QcOrderRepository;
import com.fps.svmes.services.DispatchService;
import com.fps.svmes.services.QcOrderService;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QcOrderServiceImpl implements QcOrderService {

    @Autowired
    private QcOrderRepository qcOrderRepo;

    @Autowired
    private DispatchRepository dispatchRepo;

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public QcOrderDTO createQcOrder(QcOrderRequest request, Integer userId) {
        QcOrder qcOrder = new QcOrder();
        qcOrder.setName(request.getName());

        List<Dispatch> dispatches = request.getDispatchRequestList()
                .stream()
                .map(dispatchRequest -> {
                        Dispatch dispatch = modelMapper.map(dispatchRequest, Dispatch.class);
                        dispatch.setCreationDetails(userId, 1);
                        return dispatchRepo.save(dispatch);
                    })
                .toList();

        qcOrder.setCreationDetails(userId, 1);
        qcOrder.setQcOrderDispatches(mapQcOrderDispatch(qcOrder, dispatches, userId));

        // Save QC Order with Dispatches
        QcOrder savedQcOrder = qcOrderRepo.save(qcOrder);

        // Initialize all dispatch under this order
        savedQcOrder.getQcOrderDispatches()
                .stream()
                .map(qcOrderDispatch ->
                {
                    Long id = qcOrderDispatch.getDispatch().getId();
                    dispatchService.initializeDispatch(id, () -> dispatchService.executeDispatch(id));
                    return null;
                });

//        // Map QcOrder to QcOrderDTO
//        QcOrderDTO qcOrderDTO = modelMapper.map(savedQcOrder, QcOrderDTO.class);
//
//        // Map Dispatch entities to DispatchDTO and set them in QcOrderDTO
//        List<DispatchDTO> dispatchDTOs = savedQcOrder.getQcOrderDispatches().stream()
//                .map(qcOrderDispatch -> modelMapper.map(qcOrderDispatch.getDispatch(), DispatchDTO.class))
//                .toList();
//        qcOrderDTO.setDispatches(dispatchDTOs);


        return mapQcOrderToDTO(savedQcOrder);
    }

    // Update qc order with the request payload, existing dispatch will be canceled,
    // dispatched task that are created by it but in pending state will be set to canceled state,
    // all deletion related to qc order entity will be soft delete by setting status to 0.
    @Transactional
    public QcOrderDTO updateQcOrder(Long id, QcOrderRequest request, Integer userId) {
        QcOrder qcOrder = qcOrderRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("QC Order not found"));

        qcOrder.setName(request.getName());

        // Soft-delete existing Dispatches
        qcOrder.getQcOrderDispatches().forEach(qcOrderDispatch -> {
            this.dispatchService.deleteDispatch(qcOrderDispatch.getDispatch().getId());
            qcOrderDispatch.setStatus(0);
        });
        qcOrder.getQcOrderDispatches().clear();

        // Get dispatch mapped by order's request list
        List<Dispatch> updatedDispatches = request.getDispatchRequestList()
                .stream()
                .map(dispatchRequest -> {
                    Dispatch dispatch = new Dispatch();
                    modelMapper.map(dispatchRequest, dispatch);
                    return dispatch;
                })
                .toList();

        // Set existing qc order dispatch middle table row status to 0
        qcOrder.getQcOrderDispatches().forEach(qcOrderDispatch -> qcOrderDispatch.setStatus(0));
        qcOrder.getQcOrderDispatches().clear();

        // Add new rows
        qcOrder.getQcOrderDispatches().addAll(mapQcOrderDispatch(qcOrder, updatedDispatches, userId));
        QcOrder updatedQcOrder = qcOrderRepo.save(qcOrder);
        return mapQcOrderToDTO(updatedQcOrder);
    }

    // Get Active QC Order by ID
    @Transactional(readOnly = true)
    public QcOrderDTO getQcOrderById(Long orderId) {
        QcOrder qcOrder = qcOrderRepo.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("QC Order not found"));
        return mapQcOrderToDTO(qcOrder);
    }

    // Delete QC Order
    @Transactional
    public void deleteQcOrder(Long orderId) {
        QcOrder qcOrder = qcOrderRepo.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("QC Order not found"));

        // Soft-delete QC Order
        qcOrder.setStatus(0);

        // Soft-delete related Dispatches
        qcOrder.getQcOrderDispatches().forEach(qcOrderDispatch -> {
            dispatchService.deleteDispatch(qcOrderDispatch.getDispatch().getId());
            qcOrderDispatch.setStatus(0);
        });

        qcOrderRepo.save(qcOrder);
    }

    // Get All QC Orders
    @Transactional(readOnly = true)
    public List<QcOrderDTO> getAllQcOrders() {
        List<QcOrder> qcOrders = qcOrderRepo.findAll();
        return qcOrders.stream().map(this::mapQcOrderToDTO).toList();
    }



    @Transactional
    public void pauseDispatch(Long orderId, Long dispatchId, Integer userId) {
        dispatchService.pauseDispatch(dispatchId, userId);
    }

    @Transactional
    public void resumeDispatch(Long orderId, Long dispatchId, Integer userId) {
        dispatchService.resumeDispatch(dispatchId, userId);
    }

    // Helper: Map QcOrder to DTO
    private QcOrderDTO mapQcOrderToDTO(QcOrder qcOrder) {
        QcOrderDTO dto = modelMapper.map(qcOrder, QcOrderDTO.class);
        List<DispatchDTO> dispatchDTOs = qcOrder.getQcOrderDispatches().stream()
                .map(qcOrderDispatch -> modelMapper.map(qcOrderDispatch.getDispatch(), DispatchDTO.class))
                .toList();
        dto.setDispatches(dispatchDTOs);
        return dto;
    }

    private List<QcOrderDispatch> mapQcOrderDispatch(QcOrder qcOrder, List<Dispatch> dispatches, Integer userId) {
        return dispatches.stream()
                .map(dispatch -> {
                    QcOrderDispatch qcOrderDispatch = new QcOrderDispatch(qcOrder, dispatch);
                    qcOrderDispatch.setCreationDetails(userId, 1);
                    return qcOrderDispatch;
                })
                .toList();
    }
}
