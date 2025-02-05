package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
import com.fps.svmes.dto.dtos.dispatch.QcOrderDTO;
import com.fps.svmes.dto.dtos.user.UserDTO;
import com.fps.svmes.dto.requests.DispatchRequest;
import com.fps.svmes.dto.requests.QcOrderRequest;
import com.fps.svmes.models.sql.maintenance.Equipment;
import com.fps.svmes.models.sql.maintenance.MaintenanceWorkOrder;
import com.fps.svmes.models.sql.production.Product;
import com.fps.svmes.models.sql.production.ProductionWorkOrder;
import com.fps.svmes.models.sql.production.RawMaterial;
import com.fps.svmes.models.sql.taskSchedule.*;
import com.fps.svmes.models.sql.user.User;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchRepository;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchedTaskRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Service
public class QcOrderServiceImpl implements QcOrderService {

    @Autowired private QcOrderRepository qcOrderRepo;
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
    public QcOrderDTO createQcOrder(QcOrderRequest request, Integer userId) {
        QcOrder qcOrder = new QcOrder();
        qcOrder.setName(request.getName());
        qcOrder.setDescription(request.getDescription());
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

        qcOrder.setName(request.getName());
        qcOrder.setDescription(request.getDescription());
        qcOrder.setUpdateDetails(userId, 1);

        return mapQcOrderToDTO(qcOrderRepo.save(qcOrder));
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

        // ** Soft-delete order & associated dispatches**
        qcOrder.setStatus(0);
        qcOrder.getQcOrderDispatches().forEach(qcOrderDispatch -> {
            dispatchService.deleteDispatch(qcOrderDispatch.getDispatch().getId(), userId);
            qcOrderDispatch.setStatus(0);
        });

        // Set all dispatched tasks under this order that are in pending mode (state 1) to canceled mode (state 5)
        qcOrder.getQcOrderDispatches().forEach(qcOrderDispatch -> {
            Long dispatchId = qcOrderDispatch.getDispatch().getId();

            // Fetch all dispatched tasks under this dispatch that are in pending state
            List<DispatchedTask> dispatchTasks = dispatchedTaskRepository.findByDispatchIdAndStateIdAndStatus(dispatchId, 1, 1);

            if (!dispatchTasks.isEmpty()) {
                // Update state for all fetched tasks in one go
                dispatchTasks.forEach(task -> task.setStateId((short) 5));

                // Batch save the updated tasks
                dispatchedTaskRepository.saveAll(dispatchTasks);
            }
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
        List<DispatchForm> df_list = new ArrayList<>(request.getFormIds()
                .stream()
                .map(formTreeNodeId -> new DispatchForm(dispatch, formTreeNodeId))
                .toList());

        savedDispatch.setDispatchForms(df_list);

        List<DispatchUser> du_list = new ArrayList<>(request.getUserIds()
                .stream()
                .map(uid ->
                {
                  User user = userRepository.findById(Math.toIntExact(uid)).orElseThrow(() -> new EntityNotFoundException("user not found: " + uid));
                  DispatchUser du = new DispatchUser();
                  du.setDispatch(dispatch);
                  du.setUser(user);
                  return du;
                })
                .toList());
        savedDispatch.setDispatchUsers(du_list);

        List<DispatchInstrument> di_list = new ArrayList<>(request.getInstrumentIds()
                .stream()
                .map(instrument_id ->
                {
                    DispatchInstrument di = new DispatchInstrument();
                    di.setDispatch(dispatch);
                    di.setInstrumentId(instrument_id);
                    di.setCreationDetails(userId, 1);
                    return di;
                })
                .toList());
        savedDispatch.setDispatchInstruments(di_list);

        List<DispatchTestSubject> dt_list = new ArrayList<>(request.getTestSubjectIds()
                .stream()
                .map(test_subject_id ->
                {
                    DispatchTestSubject dt = new DispatchTestSubject();
                    dt.setDispatch(dispatch);
                    dt.setTestSubjectId(test_subject_id);
                    dt.setCreationDetails(userId, 1);
                    return dt;
                })
                .toList());
        savedDispatch.setDispatchTestSubjects(dt_list);

        List<DispatchSamplingLocation> ds_list = new ArrayList<>(request.getSamplingLocationIds()
                .stream()
                .map(sampling_location_id ->
                {
                    DispatchSamplingLocation ds = new DispatchSamplingLocation();
                    ds.setDispatch(dispatch);
                    ds.setSamplingLocationId(sampling_location_id);
                    ds.setCreationDetails(userId, 1);
                    return ds;
                })
                .toList());
        savedDispatch.setDispatchSamplingLocations(ds_list);

        // Other module data association for junction table
        List<DispatchProduct> dp_list = new ArrayList<>(request.getProductIds()
                .stream()
                .map(product_id ->
                {
                    Product product = productRepository.findById(product_id).orElseThrow(() -> new EntityNotFoundException("product not found: " + product_id));
                    DispatchProduct dp = new DispatchProduct();
                    dp.setDispatch(dispatch);
                    dp.setProduct(product);
                    return dp;
                })
                .toList());
        savedDispatch.setDispatchProducts(dp_list);

        List<DispatchRawMaterial> dr_list = new ArrayList<>(request.getRawMaterialIds()
                .stream()
                .map(raw_material_id ->
                {
                    RawMaterial rawMaterial = rawMaterialRepository.findById(raw_material_id).orElseThrow(() -> new EntityNotFoundException("raw material not found: " + raw_material_id));
                    DispatchRawMaterial dr = new DispatchRawMaterial();
                    dr.setDispatch(dispatch);
                    dr.setRawMaterial(rawMaterial);
                    return dr;
                })
                .toList());
        savedDispatch.setDispatchRawMaterials(dr_list);

        List<DispatchProductionWorkOrder> pwo_list = new ArrayList<>(request.getProductionWorkOrderIds()
                .stream()
                .map(pwo_id ->
                {
                    ProductionWorkOrder productionWorkOrder = productionWorkOrderRepository.findById(pwo_id).orElseThrow(() -> new EntityNotFoundException("production work order not found: " + pwo_id));
                    DispatchProductionWorkOrder dpwo = new DispatchProductionWorkOrder();
                    dpwo.setDispatch(dispatch);
                    dpwo.setProductionWorkOrder(productionWorkOrder);
                    return dpwo;
                })
                .toList());
        savedDispatch.setDispatchProductionWorkOrders(pwo_list);

        List<DispatchMaintenanceWorkOrder> mwo_list = new ArrayList<>(request.getMaintenanceWorkOrderIds()
                .stream()
                .map(mwo_id ->
                {
                    MaintenanceWorkOrder maintenanceWorkOrder = maintenanceWorkOrderRepository.findById(mwo_id).orElseThrow(() -> new EntityNotFoundException("maintenance work order not found: " + mwo_id));
                    DispatchMaintenanceWorkOrder dmwo = new DispatchMaintenanceWorkOrder();
                    dmwo.setDispatch(dispatch);
                    dmwo.setMaintenanceWorkOrder(maintenanceWorkOrder);
                    return dmwo;
                })
                .toList());
        savedDispatch.setDispatchMaintenanceWorkOrders(mwo_list);

        List<DispatchEquipment> de_list = new ArrayList<>(request.getEquipmentIds()
                .stream()
                .map(equipment_id ->
                {
                    Equipment equipment = equipmentRepository.findById(equipment_id).orElseThrow(() -> new EntityNotFoundException("equipment not found: " + equipment_id));
                    DispatchEquipment de = new DispatchEquipment();
                    de.setDispatch(dispatch);
                    de.setEquipment(equipment);
                    return de;
                })
                .toList());
        savedDispatch.setDispatchEquipments(de_list);

        return dispatchRepo.save(savedDispatch); // Save again after setting associations
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
                .map(qcOrderDispatch ->
                {
                    Dispatch dispatch = qcOrderDispatch.getDispatch();
                    DispatchDTO dispatchDTO = modelMapper.map(qcOrderDispatch.getDispatch(), DispatchDTO.class);

                    dispatchDTO.setUsers(dispatch.getDispatchUsers()
                            .stream()
                            .map(dispatchUser -> modelMapper.map(dispatchUser.getUser(), UserDTO.class))
                            .toList());

                    dispatchDTO.setQcFormTreeNodeIds(
                            dispatch.getDispatchForms()
                            .stream()
                            .map(DispatchForm::getQcFormTreeNodeId).toList());

                    dispatchDTO.setProductIds(dispatch.getDispatchProducts()
                            .stream()
                            .map(DispatchProduct::getProduct)
                            .map(Product::getId)
                            .toList());

                    dispatchDTO.setRawMaterialIds(dispatch.getDispatchRawMaterials()
                            .stream()
                            .map(DispatchRawMaterial::getRawMaterial)
                            .map(RawMaterial::getId)
                            .toList());

                    dispatchDTO.setProductionWorkOrderIds(dispatch.getDispatchProductionWorkOrders()
                            .stream()
                            .map(DispatchProductionWorkOrder::getProductionWorkOrder)
                            .map(ProductionWorkOrder::getId)
                            .toList());

                    dispatchDTO.setEquipmentIds(dispatch.getDispatchEquipments()
                            .stream()
                            .map(DispatchEquipment::getEquipment)
                            .map(Equipment::getId)
                            .toList());

                    dispatchDTO.setMaintenanceWorkOrderIds(dispatch.getDispatchMaintenanceWorkOrders()
                            .stream()
                            .map(DispatchMaintenanceWorkOrder::getMaintenanceWorkOrder)
                            .map(MaintenanceWorkOrder::getId)
                            .toList());

                    dispatchDTO.setInstrumentIds(dispatch.getDispatchInstruments()
                            .stream()
                            .map(DispatchInstrument::getInstrumentId)
                            .toList());

                    dispatchDTO.setSamplingLocationIds(dispatch.getDispatchSamplingLocations()
                            .stream()
                            .map(DispatchSamplingLocation::getSamplingLocationId)
                            .toList());

                    dispatchDTO.setTestSubjectIds(dispatch.getDispatchTestSubjects()
                            .stream()
                            .map(DispatchTestSubject::getTestSubjectId)
                            .toList());

                    return dispatchDTO;
                })
                .toList();

        qcOrderDTO.setDispatches(dispatchDTOs);
        return qcOrderDTO;
    }
}
