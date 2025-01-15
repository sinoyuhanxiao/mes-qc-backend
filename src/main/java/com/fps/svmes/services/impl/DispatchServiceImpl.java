package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
import com.fps.svmes.dto.dtos.dispatch.DispatchedTaskDTO;
import com.fps.svmes.dto.dtos.user.UserDTO;
import com.fps.svmes.dto.requests.DispatchRequest;
import com.fps.svmes.models.sql.taskSchedule.*;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchRepository;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchedTaskRepository;
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
    private TaskScheduleService taskScheduleService;

    @Autowired
    private DispatchedTaskService dispatchedTaskService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(DispatchServiceImpl.class);

    // ------------- Dispatch CRUD -----------------------------------------------------------------------

    // Map dispatch request to a dispatch object,
    @Transactional
    public DispatchDTO createDispatch(DispatchRequest request) {
        Dispatch dispatch = modelMapper.map(request, Dispatch.class);
        dispatch.setCreationDetails(request.getCreatedBy(), 1);

        if (request.getFormIds() != null) {
            dispatch.setDispatchForms(mapDispatchForms(dispatch, request.getFormIds()));
        }

        if (request.getUserIds() != null) {
            dispatch.setDispatchUsers(mapDispatchUsers(dispatch, request.getUserIds()));
        }

        Dispatch savedDispatch = dispatchRepo.save(dispatch);

        // Schedule the task or a one-time start-time check
        try {
            this.initializeDispatch(savedDispatch.getId(), () -> executeDispatch(savedDispatch.getId()));
        } catch (IllegalStateException e) {
            logger.warn("Dispatch created but not immediately scheduled: {}", e.getMessage());
        }

        return convertToDispatchDTO(savedDispatch);
    }

    @Transactional
    public DispatchDTO createManualDispatch(DispatchRequest request) {
        Dispatch dispatch = modelMapper.map(request, Dispatch.class);
        dispatch.setExecutedCount(0);
        dispatch.setCreationDetails(request.getCreatedBy(), 1);

        if (request.getFormIds() != null) {
            dispatch.setDispatchForms(this.mapDispatchForms(dispatch, request.getFormIds()));
        }

        if (request.getUserIds() != null) {
            dispatch.setDispatchUsers(mapDispatchUsers(dispatch, request.getUserIds()));
        }

        // since manual dispatch only execute once, will default isActive to false
        dispatch.setIsActive(false);
        dispatch.setExecutedCount(dispatch.getExecutedCount() + 1); // Increment executed count
        Dispatch savedDispatch = dispatchRepo.save(dispatch);

        createTasksForDispatch(dispatch);

        return convertToDispatchDTO(savedDispatch);
    }

    @Transactional
    public DispatchDTO updateDispatch(Long id, DispatchRequest request) {
        Dispatch dispatch = dispatchRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));

        modelMapper.map(request, dispatch);
        dispatch.setUpdateDetails(request.getUpdatedBy(), 1);

        // Handle DispatchForms
        if (request.getFormIds() != null) {
            // Clear old forms
            dispatch.getDispatchForms().forEach(form -> form.setStatus(dispatch.getStatus())); // Update status
            dispatch.getDispatchForms().clear();

            // Add new forms
            List<DispatchForm> forms = mapDispatchForms(dispatch, request.getFormIds());
            dispatch.getDispatchForms().addAll(forms);
        }

        // Handle DispatchUser
        if (request.getUserIds() != null) {
            dispatch.getDispatchUsers().forEach(user -> user.setStatus(dispatch.getStatus())); // Update status
            dispatch.getDispatchUsers().clear();

            List<DispatchUser> users = mapDispatchUsers(dispatch, request.getUserIds());
            dispatch.getDispatchUsers().addAll(users);
        }

        Dispatch updatedDispatch = dispatchRepo.save(dispatch);

        // Update scheduled task
        if (Objects.equals(request.getType(), "SCHEDULED") && isDispatchExistAndActive(updatedDispatch)) {
            // reset its scheduled task
            taskScheduleService.removeAllTasks(dispatch.getId());

            initializeDispatch(id, () -> executeDispatch(updatedDispatch.getId()));
        }

        return convertToDispatchDTO(updatedDispatch);
    }

    @Transactional
    public void deleteDispatch(Long id) {
        Dispatch dispatch = dispatchRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));

        // Cancel any task of this dispatch if scheduled
        boolean taskCancelled = taskScheduleService.removeAllTasks(id);
        if (taskCancelled) {
            logger.info("Cancelled all scheduled task for Dispatch ID: {}", id);
        }

        dispatch.setIsActive(false);
        dispatch.setStatus(0);

        // Update the status of related personnel and forms
        if (dispatch.getDispatchUsers() != null) {
            dispatch.getDispatchUsers().forEach(personnel -> personnel.setStatus(0));
        }
        if (dispatch.getDispatchForms() != null) {
            dispatch.getDispatchForms().forEach(form -> form.setStatus(0));
        }

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
                        dispatch.getIsActive())
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
            if (dispatch.getIsActive()) {
                dispatch.setIsActive(false);
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
            dispatch.setIsActive(false);
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
            dispatch.setIsActive(false);
            dispatchRepo.save(dispatch);
        } else {
            logger.info("No task was scheduled for this dispatch ID.");
        }
    }

    // // ------------- Helper Function  -----------------------------------------------------------------------

    private boolean isDispatchExistAndActive(Dispatch dispatch) {
        return dispatch.getIsActive() && (dispatch.getStatus() == 1);
    }

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
                    .map(DispatchForm::getQcFormTreeNodeId)
                    .toList());
        }

        // Map dispatch_personnel to list of UserDTOs
        if (dispatch.getDispatchUsers() != null) {
            dto.setUsers(dispatch.getDispatchUsers().stream()
                    .map(personnel -> modelMapper.map(personnel.getUser(), UserDTO.class))
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

        // Loop through forms and personnel, and insert tasks
        for (DispatchForm form : dispatch.getDispatchForms()) {
            taskDTO.setQcFormTreeNodeId(form.getQcFormTreeNodeId());

            // Extract user IDs from personnel
            List<Integer> userIds = dispatch.getDispatchUsers().stream()
                    .map(user -> user.getUser().getId())
                    .collect(Collectors.toList());

            // Use the service to insert tasks
            dispatchedTaskService.insertDispatchedTasks(taskDTO, userIds);
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

}