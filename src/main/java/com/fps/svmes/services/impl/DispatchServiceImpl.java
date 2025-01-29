package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.dispatch.*;
import com.fps.svmes.dto.dtos.user.UserDTO;
import com.fps.svmes.dto.requests.DispatchRequest;
import com.fps.svmes.models.sql.maintenance.Equipment;
import com.fps.svmes.models.sql.maintenance.MaintenanceWorkOrder;
import com.fps.svmes.models.sql.production.Product;
import com.fps.svmes.models.sql.production.ProductionWorkOrder;
import com.fps.svmes.models.sql.production.RawMaterial;
import com.fps.svmes.models.sql.taskSchedule.*;
import com.fps.svmes.models.sql.user.User;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchRepository;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchedTaskRepository;
import com.fps.svmes.repositories.jpaRepo.maintenance.EquipmentRepository;
import com.fps.svmes.repositories.jpaRepo.maintenance.MaintenanceWorkOrderRepository;
import com.fps.svmes.repositories.jpaRepo.production.ProductRepository;
import com.fps.svmes.repositories.jpaRepo.production.ProductionWorkOrderRepository;
import com.fps.svmes.repositories.jpaRepo.production.RawMaterialRepository;
import com.fps.svmes.services.DispatchService;
import com.fps.svmes.services.DispatchedTaskService;
import com.fps.svmes.services.TaskScheduleService;
import com.fps.svmes.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.modelmapper.ModelMapper;

import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the DispatchService interface.
 */
@Service
public class DispatchServiceImpl implements DispatchService {

    @Autowired
    private DispatchRepository dispatchRepo;

    @Autowired
    private DispatchedTaskRepository dispatchedTaskRepo;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RawMaterialRepository rawMaterialRepository;

    @Autowired
    private ProductionWorkOrderRepository productionWorkOrderRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private MaintenanceWorkOrderRepository maintenanceWorkOrderRepository;

    @Autowired
    private TaskScheduleService taskScheduleService;

    @Autowired
    private DispatchedTaskService dispatchedTaskService;

    @Autowired
    private UserService userService;

    @Autowired
    private ModelMapper modelMapper;


    private static final Logger logger = LoggerFactory.getLogger(DispatchServiceImpl.class);

    // ------------- Dispatch CRUD -----------------------------------------------------------------------

    // Map dispatch request to a dispatch object,
    @Transactional
    public DispatchDTO createDispatch(DispatchRequest request) {
        logger.info("Running createDispatch");

        Dispatch dispatch = modelMapper.map(request, Dispatch.class);

        // TODO: add userId in this function
        dispatch.setCreationDetails(14, 1);

        // Initialize mutable collections
        List<DispatchForm> mutableDispatchForms = new ArrayList<>();
        List<DispatchUser> mutableDispatchUsers = new ArrayList<>();
        List<DispatchProduct> mutableDispatchProducts = new ArrayList<>();
        List<DispatchRawMaterial> mutableDispatchRawMaterials = new ArrayList<>();
        List<DispatchProductionWorkOrder> mutableDispatchProductionWorkOrders = new ArrayList<>();
        List<DispatchEquipment> mutableDispatchEquipments = new ArrayList<>();
        List<DispatchMaintenanceWorkOrder> mutableDispatchMaintenanceWorkOrders = new ArrayList<>();

        if (request.getFormIds() != null) {
            mutableDispatchForms.addAll(mapDispatchForms(dispatch, request.getFormIds()));
        }

        if (request.getUserIds() != null) {
            mutableDispatchUsers.addAll(mapDispatchUsers(dispatch, request.getUserIds()));
        }

        if (request.getProductIds() != null) {
            mutableDispatchProducts.addAll(mapDispatchProducts(dispatch, request.getProductIds()));
        }

        if (request.getRawMaterialIds() != null) {
            mutableDispatchRawMaterials.addAll(mapDispatchRawMaterials(dispatch, request.getRawMaterialIds()));
        }

        if (request.getProductionWorkOrderIds() != null) {
            mutableDispatchProductionWorkOrders.addAll(mapDispatchProductionWorkOrders(dispatch, request.getProductionWorkOrderIds()));
        }

        if (request.getEquipmentIds() != null) {
            mutableDispatchEquipments.addAll(mapDispatchEquipments(dispatch, request.getEquipmentIds()));
        }

        if (request.getMaintenanceWorkOrderIds() != null) {
            mutableDispatchMaintenanceWorkOrders.addAll(mapDispatchMaintenanceWorkOrders(dispatch, request.getMaintenanceWorkOrderIds()));
        }

        // Set the collections back to the entity
        dispatch.setDispatchForms(mutableDispatchForms);
        dispatch.setDispatchUsers(mutableDispatchUsers);
        dispatch.setDispatchProducts(mutableDispatchProducts);
        dispatch.setDispatchRawMaterials(mutableDispatchRawMaterials);
        dispatch.setDispatchProductionWorkOrders(mutableDispatchProductionWorkOrders);
        dispatch.setDispatchEquipments(mutableDispatchEquipments);
        dispatch.setDispatchMaintenanceWorkOrders(mutableDispatchMaintenanceWorkOrders);


        if (dispatch.getType().equals("MANUAL")) {
            // since manual dispatch only execute once, will default isActive to false
                dispatch.setState(DispatchState.Inactive.getState());
                dispatch.setExecutedCount(dispatch.getExecutedCount() + 1); // Increment executed count
        }

        Dispatch savedDispatch = dispatchRepo.save(dispatch);

        // Insert rows to dispatched task table
        try {
            if (dispatch.getType().equals("MANUAL")) {
                createTasksForDispatch(savedDispatch);
            } else {
                this.initializeDispatch(savedDispatch.getId(), () -> executeDispatch(savedDispatch.getId()));
            }
        } catch (IllegalStateException e) {
            logger.warn("Dispatch created but not immediately scheduled: {}", e.getMessage());
        }

        return convertToDispatchDTO(savedDispatch);
    }

    @Transactional
    public DispatchDTO updateDispatch(Long id, DispatchRequest request) {
        logger.info("Running updateDispatch for Dispatch ID: {}", id);

        Dispatch dispatch = dispatchRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));

        // TODO: Sync dispatched task previously created by this dispatch. Adjust state for pending task to cancelled.
        // TODO: add userId in this function
        modelMapper.map(request, dispatch);
        dispatch.setUpdateDetails(14, 1);

        // Clear and update associations with proper handling
        updateDispatchForms(dispatch, request.getFormIds());
        updateDispatchUsers(dispatch, request.getUserIds());
        updateDispatchProducts(dispatch, request.getProductIds());
        updateDispatchRawMaterials(dispatch, request.getRawMaterialIds());
        updateDispatchProductionWorkOrders(dispatch, request.getProductionWorkOrderIds());
        updateDispatchEquipments(dispatch, request.getEquipmentIds());
        updateDispatchMaintenanceWorkOrders(dispatch, request.getMaintenanceWorkOrderIds());


        if (dispatch.getType().equals("MANUAL")) {
            // since manual dispatch only execute once, will default isActive to false
            dispatch.setState(DispatchState.Inactive.getState());
            dispatch.setExecutedCount(dispatch.getExecutedCount() + 1); // Increment executed count
        }

        Dispatch updatedDispatch = dispatchRepo.save(dispatch);

        // TODO: If there are dispatched task created by this dispatch, set all the "pending" dispatched task by this dispatch to "cancelled"

        if (updatedDispatch.getStatus() == 1) {
            if ("SCHEDULED".equals(request.getType())) {
                // reset its scheduled task
                taskScheduleService.removeAllTasks(dispatch.getId());
                initializeDispatch(id, () -> executeDispatch(updatedDispatch.getId()));
            } else {
                // manual dispatch will just create and insert row to dispatched task table
                createTasksForDispatch(updatedDispatch);
            }
        }

        return convertToDispatchDTO(updatedDispatch);
    }

    @Transactional
    public void deleteDispatch(Long id) {
        logger.info("Running deleteDispatch for Dispatch ID: {}", id);
        Dispatch dispatch = dispatchRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));

        // Cancel any task of this dispatch if scheduled
        boolean taskCancelled = taskScheduleService.removeAllTasks(id);
        if (taskCancelled) {
            logger.info("Cancelled all scheduled task for Dispatch ID: {}", id);
        }

        dispatch.setState(DispatchState.Inactive.getState());
        dispatch.setStatus(0);

        // Update the status of related personnel and forms
        if (dispatch.getDispatchUsers() != null) {
            dispatch.getDispatchUsers().forEach(user -> user.setStatus(0));
        }
        if (dispatch.getDispatchForms() != null) {
            dispatch.getDispatchForms().forEach(form -> form.setStatus(0));
        }
        if (dispatch.getDispatchProducts() != null) {
            dispatch.getDispatchProducts().forEach(product -> product.setStatus((short) 0));
        }
        if (dispatch.getDispatchRawMaterials() != null) {
            dispatch.getDispatchRawMaterials().forEach(rawMaterial -> rawMaterial.setStatus((short) 0));
        }
        if (dispatch.getDispatchProductionWorkOrders() != null) {
            dispatch.getDispatchProductionWorkOrders().forEach(workOrder -> workOrder.setStatus((short) 0));
        }
        if (dispatch.getDispatchEquipments() != null) {
            dispatch.getDispatchEquipments().forEach(equipment -> equipment.setStatus((short) 0));
        }
        if (dispatch.getDispatchMaintenanceWorkOrders() != null) {
            dispatch.getDispatchMaintenanceWorkOrders().forEach(maintenanceWorkOrder -> maintenanceWorkOrder.setStatus((short) 0));
        }

        // TODO: If there are dispatched task created by this dispatch, adjust dispatched task that are in pending to cancelled.

        dispatch.setUpdatedAt(OffsetDateTime.now());
        dispatchRepo.save(dispatch);
        logger.warn("Soft-deleted Dispatch ID: {}", id);
    }

    @Transactional(readOnly = true)
    public DispatchDTO getDispatch(Long id) {
        Dispatch dispatch = dispatchRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch with ID " + id + " not found"));

        return convertToDispatchDTO(dispatch);
    }

    @Transactional(readOnly = true)
    public List<DispatchDTO> getAllDispatches() {
        return dispatchRepo.findAll().stream()
                .map(this::convertToDispatchDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DispatchedTaskDTO> getAllDispatchedTasks() {
        // Fetch all dispatched tasks from the repository
        List<DispatchedTask> dispatchedTasks = dispatchedTaskRepo.findAll();

        return dispatchedTasks.stream()
                .map(dispatchedTask -> {
                    try {
                        // Skip if dispatch or user is missing
                        if (dispatchedTask.getDispatch() == null || dispatchedTask.getUser() == null) {
                            return null; // Skip this entry
                        }

                        DispatchedTaskDTO dto = modelMapper.map(dispatchedTask, DispatchedTaskDTO.class);
                        dto.setDispatchId(dispatchedTask.getDispatch().getId());
                        dto.setUserId(Long.valueOf(dispatchedTask.getUser().getId()));

                        return dto;
                    } catch (EntityNotFoundException e) {
                        // Log and skip the entry
                        return null;
                    }
                })
                .filter(Objects::nonNull) // Remove skipped entries
                .collect(Collectors.toList());
    }

    // ------------- SCHEDULING LOGIC -----------------------------------------------------------------------

    // Runs on server start up
    @EventListener(ApplicationReadyEvent.class)
    public void initializeSchedules() {
        logger.info("Initializing scheduled tasks...");
        scheduleDispatches();
        logger.info("Done initializing scheduling dispatches...");

    }

    // Go through all existing dispatch to initialize scheduling
    @Override
    public void scheduleDispatches() {
        dispatchRepo.findByStatus(1).stream()
                .filter(dispatch -> "SCHEDULED".equals(dispatch.getType()) &&
                        dispatch.getStatus() != null && dispatch.getStatus() == 1 &&
                        !taskScheduleService.isScheduled(dispatch.getId()))
                .forEach(dispatch -> {
                    try {
                        initializeDispatch(dispatch.getId(), () -> executeDispatch(dispatch.getId()));
                    } catch (Exception e) {
                        logger.error("Failed to schedule task for Dispatch ID: {}", dispatch.getId(), e);
                    }
                });
    }

    // Runs every 10 minutes (600000 ms)
    @Scheduled(fixedRate = 600000)
    public void periodicCleanupExpiredTasks() {
        logger.info("Running cleanup of expired tasks...");

        dispatchRepo.findByStatus(1).stream()
                .filter(dispatch -> "SCHEDULED".equals(dispatch.getType()) &&
                        dispatch.getState().equals(DispatchState.Active.getState()))
                .forEach(dispatch -> {
                    if (dispatch.getEndTime().isBefore(OffsetDateTime.now())) {
                        logger.info("Dispatch ID {} has expired. Canceling task...", dispatch.getId());
                        cancelDispatchTask(dispatch.getId());
                    }
                });
    }

    // called by create dispatch, update dispatch, initialization upon server restart , schedule by id
    // This function setup task(CRON, FUTURE, CANCEL) based on the start/end time of dispatch
    public void initializeDispatch(Long dispatchId, Runnable task) {
        Dispatch dispatch = dispatchRepo.findById(dispatchId)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));

        OffsetDateTime now = OffsetDateTime.now();

        // Dispatch is already scheduled
        if (taskScheduleService.isScheduled(dispatchId)){
            logger.info("Dispatch ID {} is already scheduled.", dispatchId);
            return;
        }

        if (dispatch.getEndTime().isBefore(now)) {
            logger.info("Dispatch ID {} has an end time in the past. Skipping scheduling.", dispatchId);
            // set dispatch to be inactive
            if (dispatch.getState() == DispatchState.Active.getState()) {
                dispatch.setState(DispatchState.Expired.getState());
                dispatch.setUpdatedAt(OffsetDateTime.now());
                dispatchRepo.save(dispatch);
            }
            return;
        }

        if (dispatch.getStartTime().isAfter(now)) {
            taskScheduleService.scheduleFutureDispatch(dispatch, task);
        } else {
            taskScheduleService.scheduleDispatch(dispatch, task);
        }
    }

    // This function updates isActive where needed, and insert rows to dispatched task table
    @Transactional
    @Override
    public void executeDispatch(Long dispatchId) {
        Dispatch dispatch = this.getDispatchWithDetails(dispatchId);

        // cancel and update is active for dispatch reach limit
        if (hasReachedDispatchLimit(dispatch)) {
            if(taskScheduleService.isScheduled(dispatch.getId())) {
                taskScheduleService.removeAllTasks(dispatch.getId());
            }
            dispatch.setState(DispatchState.Exhausted.getState());
            dispatchRepo.save(dispatch);
            return;
        }

        try {
            // insert dispatched task rows and increase executed count
            processDispatch(dispatch);
            dispatchRepo.save(dispatch);
        } catch (Exception e) {
            logger.error("Error executing Dispatch ID: {}", dispatchId, e);
        }
    }


    // This function cancels all tasks of a dispatch
    @Override
    public void cancelDispatchTask(Long dispatchId) {
        Dispatch dispatch = dispatchRepo.findById(dispatchId)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));

        if (taskScheduleService.removeAllTasks(dispatchId)) {
            // Update status after cancellation
            dispatch.setUpdatedAt(OffsetDateTime.now());
            dispatch.setState(DispatchState.Inactive.getState());
            dispatchRepo.save(dispatch);
        } else {
            logger.info("No task was scheduled for this dispatch ID.");
        }
    }

    // This function cancels all tasks of a dispatch
    @Override
    public void pauseDispatch(Long dispatchId, Integer userId) {
        Dispatch dispatch = dispatchRepo.findById(dispatchId)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));

        if (taskScheduleService.removeTask(dispatchId, TaskType.CRON)) {
            // Update status after cancellation
            dispatch.setUpdatedAt(OffsetDateTime.now());
            dispatch.setState(DispatchState.Paused.getState());
            dispatchRepo.save(dispatch);
        } else {
            logger.info("No task was scheduled for this dispatch ID.");
        }
    }

    public void resumeDispatch(Long dispatchId, Integer userId) {
        Dispatch dispatch = dispatchRepo.findById(dispatchId)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));

        taskScheduleService.setupCronTask(dispatch,() -> executeDispatch(dispatch.getId()));
        dispatch.setUpdatedAt(OffsetDateTime.now());
        dispatch.setState(DispatchState.Active.getState());
        dispatchRepo.save(dispatch);
    }

    // // ------------- Helper Function  -----------------------------------------------------------------------

    private boolean hasReachedDispatchLimit(Dispatch dispatch) {
        return dispatch.getDispatchLimit() != -1 && dispatch.getExecutedCount() >= dispatch.getDispatchLimit();
    }

    private void processDispatch(Dispatch dispatch) {
        createTasksForDispatch(dispatch);
        dispatch.setExecutedCount(dispatch.getExecutedCount() + 1); // Increment executed count
    }

    @Transactional
    public Dispatch getDispatchWithDetails(Long id) {
        Dispatch dispatch = dispatchRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));
        dispatchRepo.findByIdWithForms(id).ifPresent(fetched -> dispatch.setDispatchForms(fetched.getDispatchForms()));
        dispatchRepo.findByIdWithUsers(id).ifPresent(fetched -> dispatch.setDispatchUsers(fetched.getDispatchUsers()));
        return dispatch;
    }

    private DispatchDTO convertToDispatchDTO(Dispatch dispatch) {
        DispatchDTO dto = modelMapper.map(dispatch, DispatchDTO.class);

        // Map dispatch_forms to list of form tree node IDs
        if (dispatch.getDispatchForms() != null) {
            dto.setQcFormTreeNodeIds(dispatch.getDispatchForms().stream()
                    .filter(form -> form.getStatus() == 1)
                    .map(DispatchForm::getQcFormTreeNodeId)
                    .toList());
        }

        // Map dispatch_personnel to list of UserDTOs
        if (dispatch.getDispatchUsers() != null) {
            dto.setUsers(dispatch.getDispatchUsers().stream()
                    .filter(product -> product.getStatus() == 1)
                    .map(personnel -> modelMapper
                    .map(personnel.getUser(), UserDTO.class))
                    .toList());
        }

        // map products to product_ids
        if (dispatch.getDispatchProducts() != null) {
            dto.setProductIds(dispatch.getDispatchProducts().stream()
                    .filter(product -> product.getStatus() == 1)
                    .map(DispatchProduct::getProduct)
                    .map(Product::getId)
                    .toList());
        }

        if (dispatch.getDispatchRawMaterials() != null) {
            dto.setRawMaterialIds(dispatch.getDispatchRawMaterials().stream()
                    .filter(rawMaterial -> rawMaterial.getStatus() == 1)
                    .map(DispatchRawMaterial::getRawMaterial)
                    .map(RawMaterial::getId)
                    .toList());
        }

        // map entity to ids
        if (dispatch.getDispatchProductionWorkOrders() != null) {
            dto.setProductionWorkOrderIds(dispatch.getDispatchProductionWorkOrders().stream()
                    .filter(workOrder -> workOrder.getStatus() == 1)
                    .map(DispatchProductionWorkOrder::getProductionWorkOrder)
                    .map(ProductionWorkOrder::getId)
                    .toList());
        }

        // map entity to ids
        if (dispatch.getDispatchEquipments() != null) {
            dto.setEquipmentIds(dispatch.getDispatchEquipments().stream()
                    .filter(equipment -> equipment.getStatus() == 1)
                    .map(DispatchEquipment::getEquipment)
                    .map(Equipment::getId)
                    .toList());
        }

        // map entity to ids
        if (dispatch.getDispatchMaintenanceWorkOrders() != null) {
            dto.setMaintenanceWorkOrderIds(dispatch.getDispatchMaintenanceWorkOrders().stream()
                    .filter(workOrder -> workOrder.getStatus() == 1)
                    .map(DispatchMaintenanceWorkOrder::getMaintenanceWorkOrder)
                    .map(MaintenanceWorkOrder::getId)
                    .toList());
        }

        return dto;
    }

    private void createTasksForDispatch(Dispatch dispatch) {
        OffsetDateTime dispatchTime = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime dueDate = calculateDueDate(dispatchTime, dispatch.getDueDateOffsetMinute());

        // Prepare a DispatchedTaskDTO template based on dispatch
        DispatchedTaskDTO taskDTO = new DispatchedTaskDTO();
        taskDTO.setDispatchId(dispatch.getId());
        taskDTO.setDispatchTime(dispatchTime);
        taskDTO.setName(dispatch.getName());
        taskDTO.setDescription(dispatch.getRemark());
        taskDTO.setDueDate(dueDate);
        taskDTO.setIsOverdue(false);
        taskDTO.setStateId((short) 1); // Default state ID
        taskDTO.setCreationDetails(dispatch.getCreatedBy(), 1);
        taskDTO.setNotes(dispatch.getRemark());

        // Filter DispatchForms with status = 1
        List<DispatchForm> activeForms = dispatch.getDispatchForms().stream()
                .filter(form -> form.getStatus() == 1)
                .toList();

        // Filter DispatchUsers with status = 1
        List<Integer> activeUserIds = dispatch.getDispatchUsers().stream()
                .filter(user -> user.getStatus() == 1)
                .map(user -> user.getUser().getId())
                .toList();

        // Loop through active forms and create tasks for active users
        for (DispatchForm form : activeForms) {
            taskDTO.setQcFormTreeNodeId(form.getQcFormTreeNodeId());

            // Use the service to insert tasks
            dispatchedTaskService.insertDispatchedTasks(taskDTO, activeUserIds);
        }

        logger.info("Executed Dispatch ID: {}, Created {} tasks.", dispatch.getId(), dispatch.getDispatchForms().size());
    }

    private OffsetDateTime calculateDueDate(OffsetDateTime dispatchTime, int dueDateOffsetMinute) {
        return dispatchTime.plusMinutes(dueDateOffsetMinute);
    }

    private List<DispatchForm> mapDispatchForms(Dispatch dispatch, List<String> formIds) {
        return formIds.stream()
                .map(formTreeNodeId -> new DispatchForm(dispatch, formTreeNodeId))
                .toList();
    }

    private List<DispatchUser> mapDispatchUsers(Dispatch dispatch, List<Integer> userIds) {
        return userIds.stream()
                .map(userId -> new DispatchUser(dispatch, userId))
                .toList();
    }

    private List<DispatchProduct> mapDispatchProducts(Dispatch dispatch, List<Integer> ids) {
        if (ids == null) return new ArrayList<>();
        return ids.stream()
                .map(id -> {
                    DispatchProduct dp = new DispatchProduct();
                    dp.setDispatch(dispatch);
                    dp.setProduct(productRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id)));
                    dp.setStatus((short) 1); // Default active status
                    return dp;
                })
                .toList();
    }

    private List<DispatchRawMaterial> mapDispatchRawMaterials(Dispatch dispatch, List<Integer> ids) {
        if (ids == null) return new ArrayList<>();
        return ids.stream()
                .map(id -> {
                    DispatchRawMaterial dp = new DispatchRawMaterial();
                    dp.setDispatch(dispatch);
                    dp.setRawMaterial(rawMaterialRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Raw Material not found: " + id)));
                    dp.setStatus((short) 1); // Default active status
                    return dp;
                })
                .toList();
    }

    private List<DispatchProductionWorkOrder> mapDispatchProductionWorkOrders(Dispatch dispatch, List<Integer> ids) {
        if (ids == null) return new ArrayList<>();
        return ids.stream()
                .map(id -> {
                    DispatchProductionWorkOrder dp = new DispatchProductionWorkOrder();
                    dp.setDispatch(dispatch);
                    dp.setProductionWorkOrder(productionWorkOrderRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Production Work Order not found: " + id)));
                    dp.setStatus((short) 1); // Default active status
                    return dp;
                })
                .toList();
    }

    private List<DispatchEquipment> mapDispatchEquipments(Dispatch dispatch, List<Short> ids) {
        if (ids == null) return new ArrayList<>();
        return ids.stream()
                .map(id -> {
                    DispatchEquipment dp = new DispatchEquipment();
                    dp.setDispatch(dispatch);
                    dp.setEquipment(equipmentRepository.findById(Integer.valueOf(id))
                            .orElseThrow(() -> new EntityNotFoundException("Equipment not found: " + id)));
                    dp.setStatus((short) 1); // Default active status
                    return dp;
                })
                .toList();
    }

    private List<DispatchMaintenanceWorkOrder> mapDispatchMaintenanceWorkOrders(Dispatch dispatch, List<Integer> ids) {
        if (ids == null) return new ArrayList<>();
        return ids.stream()
                .map(id -> {
                    DispatchMaintenanceWorkOrder dp = new DispatchMaintenanceWorkOrder();
                    dp.setDispatch(dispatch);
                    dp.setMaintenanceWorkOrder(maintenanceWorkOrderRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id)));
                    dp.setStatus((short) 1); // Default active status
                    return dp;
                })
                .toList();
    }

    private void updateDispatchForms(Dispatch dispatch, List<String> formIds) {
        if (formIds == null) return;

        // Mark all existing rows as inactive
        dispatch.getDispatchForms().forEach(form -> form.setStatus(0));

        // Reactivate or add new rows
        formIds.forEach(id -> {
            DispatchForm existing = dispatch.getDispatchForms().stream()
                    .filter(form -> form.getQcFormTreeNodeId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                // Reactivate if already exists
                existing.setStatus(1);
            } else {
                // Add new row
                DispatchForm newForm = new DispatchForm(dispatch, id);
                newForm.setStatus(1); // Default active status
                dispatch.getDispatchForms().add(newForm);
            }
        });}

    private void updateDispatchUsers(Dispatch dispatch, List<Integer> userIds) {
        if (userIds == null) return;

        // Get existing users with status = 1
        List<Integer> existingUsers = dispatch.getDispatchUsers().stream()
                .filter(user -> user.getStatus() == 1)
                .map(DispatchUser::getUser)
                .map(User::getId)
                .toList();

        // Reactivate or add new rows for the provided user IDs
        userIds.forEach(id -> {
            DispatchUser existing = dispatch.getDispatchUsers().stream()
                    .filter(du -> du.getUser().getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                // Reactivate if already exists but marked inactive
                existing.setStatus(1);
            } else {
                // Add new row if not already associated
                DispatchUser newUser = new DispatchUser(dispatch, id);
                newUser.setStatus(1);
                dispatch.getDispatchUsers().add(newUser);
            }
        });

        // Deactivate rows that are no longer associated
        dispatch.getDispatchUsers().stream()
                .filter(user -> !userIds.contains(user.getUser().getId()))
                .forEach(user -> user.setStatus(0));
    }

    private void updateDispatchProducts(Dispatch dispatch, List<Integer> productIds) {
        if (productIds == null) return;

        // Mark all existing rows as inactive
        dispatch.getDispatchProducts().forEach(dp -> dp.setStatus((short) 0));

        // Reactivate or add new rows
        productIds.forEach(id -> {
            DispatchProduct existing = dispatch.getDispatchProducts().stream()
                    .filter(dp -> dp.getProduct().getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                // Reactivate if already exists
                existing.setStatus((short) 1);
            } else {
                // Add new row
                DispatchProduct dp = new DispatchProduct();
                dp.setDispatch(dispatch);
                dp.setProduct(productRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id)));
                dp.setStatus((short) 1);
                dispatch.getDispatchProducts().add(dp);
            }
        });
    }


    private void updateDispatchRawMaterials(Dispatch dispatch, List<Integer> rawMaterialIds) {
        if (rawMaterialIds == null) return;

        // Mark all existing rows as inactive
        dispatch.getDispatchRawMaterials().forEach(drm -> drm.setStatus((short) 0));

        // Reactivate or add new rows
        rawMaterialIds.forEach(id -> {
            DispatchRawMaterial existing = dispatch.getDispatchRawMaterials().stream()
                    .filter(drm -> drm.getRawMaterial().getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                // Reactivate if already exists
                existing.setStatus((short) 1);
            } else {
                // Add new row
                DispatchRawMaterial drm = new DispatchRawMaterial();
                drm.setDispatch(dispatch);
                drm.setRawMaterial(rawMaterialRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Raw Material not found: " + id)));
                drm.setStatus((short) 1);
                dispatch.getDispatchRawMaterials().add(drm);
            }
        });
    }


    private void updateDispatchProductionWorkOrders(Dispatch dispatch, List<Integer> productionWorkOrderIds) {
        if (productionWorkOrderIds == null) return;

        // Mark all existing rows as inactive
        dispatch.getDispatchProductionWorkOrders().forEach(dpwo -> dpwo.setStatus((short) 0));

        // Reactivate or add new rows
        productionWorkOrderIds.forEach(id -> {
            DispatchProductionWorkOrder existing = dispatch.getDispatchProductionWorkOrders().stream()
                    .filter(dpwo -> dpwo.getProductionWorkOrder().getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                // Reactivate if already exists
                existing.setStatus((short) 1);
            } else {
                // Add new row
                DispatchProductionWorkOrder dpwo = new DispatchProductionWorkOrder();
                dpwo.setDispatch(dispatch);
                dpwo.setProductionWorkOrder(productionWorkOrderRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Production Work Order not found: " + id)));
                dpwo.setStatus((short) 1);
                dispatch.getDispatchProductionWorkOrders().add(dpwo);
            }
        });
    }

    private void updateDispatchEquipments(Dispatch dispatch, List<Short> equipmentIds) {
        if (equipmentIds == null) return;

        // Mark all existing rows as inactive
        dispatch.getDispatchEquipments().forEach(de -> de.setStatus((short) 0));

        // Reactivate or add new rows
        equipmentIds.forEach(id -> {
            DispatchEquipment existing = dispatch.getDispatchEquipments().stream()
                    .filter(de -> de.getEquipment().getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                // Reactivate if already exists
                existing.setStatus((short) 1);
            } else {
                // Add new row
                DispatchEquipment de = new DispatchEquipment();
                de.setDispatch(dispatch);
                de.setEquipment(equipmentRepository.findById(Integer.valueOf(id))
                        .orElseThrow(() -> new EntityNotFoundException("Equipment not found: " + id)));
                de.setStatus((short) 1);
                dispatch.getDispatchEquipments().add(de);
            }
        });
    }



    private void updateDispatchMaintenanceWorkOrders(Dispatch dispatch, List<Integer> maintenanceWorkOrderIds) {
        if (maintenanceWorkOrderIds == null) return;

        // Mark all existing rows as inactive
        dispatch.getDispatchMaintenanceWorkOrders().forEach(dmwo -> dmwo.setStatus((short) 0));

        // Reactivate or add new rows
        maintenanceWorkOrderIds.forEach(id -> {
            DispatchMaintenanceWorkOrder existing = dispatch.getDispatchMaintenanceWorkOrders().stream()
                    .filter(dmwo -> dmwo.getMaintenanceWorkOrder().getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                // Reactivate if already exists
                existing.setStatus((short) 1);
            } else {
                // Add new row
                DispatchMaintenanceWorkOrder dmwo = new DispatchMaintenanceWorkOrder();
                dmwo.setDispatch(dispatch);
                dmwo.setMaintenanceWorkOrder(maintenanceWorkOrderRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Maintenance Work Order not found: " + id)));
                dmwo.setStatus((short) 1);
                dispatch.getDispatchMaintenanceWorkOrders().add(dmwo);
            }
        });
    }




}