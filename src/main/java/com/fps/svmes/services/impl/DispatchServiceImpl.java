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


    @EventListener(ApplicationReadyEvent.class)
    public void initializeSchedules() {
        logger.info("Initializing scheduled tasks...");
        scheduleDispatches();
    }

    @Override
    public void scheduleDispatches() {
        dispatchRepo.findByStatus(1).stream()
                .filter(dispatch -> "SCHEDULED".equals(dispatch.getType()) && !taskScheduleService.isScheduled(dispatch.getId()))
                .forEach(dispatch -> {
                    try {
                        scheduleDispatchTask(dispatch.getId(), () -> executeDispatch(dispatch.getId()));
                    } catch (Exception e) {
                        logger.error("Failed to schedule task for Dispatch ID: {}", dispatch.getId(), e);
                    }
                });
    }


    @Transactional
    @Override
    public void executeDispatch(Long dispatchId) {
        Dispatch dispatch = this.getDispatchWithDetails(dispatchId);

        // Handle inactive dispatch
        if (dispatch.getStatus() == 0) {
            handleInactiveDispatch(dispatchId);
            return;
        }

        try {
            if (hasReachedDispatchLimit(dispatch)) {
                handleDispatchLimitReached(dispatchId, dispatch);
            } else {
                processDispatch(dispatch);
            }

            dispatchRepo.save(dispatch);
        } catch (Exception e) {
            logger.error("Error executing Dispatch ID: {}", dispatchId, e);
        }
    }

    @Transactional
    public DispatchDTO createDispatch(DispatchRequest request) {
        Dispatch dispatch = modelMapper.map(request, Dispatch.class);
        dispatch.setExecutedCount(0);
        dispatch.setCreationDetails(request.getCreatedBy(), 1);

        // Handle DispatchForms
        if (request.getFormIds() != null) {
            List<DispatchForm> forms = request.getFormIds().stream()
                    .map(formTreeNodeId -> new DispatchForm(dispatch, formTreeNodeId))
                    .toList();
            dispatch.setDispatchForms(forms);
        }

        // Handle DispatchUser
        if (request.getUserIds() != null) {
            List<DispatchUser> users = request.getUserIds().stream()
                    .map(userId -> new DispatchUser(dispatch, userId))
                    .toList();
            dispatch.setDispatchUsers(users);
        }

        Dispatch savedDispatch = dispatchRepo.save(dispatch);

        // Schedule the task or a one-time start-time check
        try {
            this.scheduleDispatchTask(savedDispatch.getId(), () -> executeDispatch(savedDispatch.getId()));
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

        // Handle DispatchForms
        if (request.getFormIds() != null) {
            List<DispatchForm> forms = request.getFormIds().stream()
                    .map(formTreeNodeId -> new DispatchForm(dispatch, formTreeNodeId))
                    .toList();
            dispatch.setDispatchForms(forms);
        }

        // Handle DispatchUser
        if (request.getUserIds() != null) {
            List<DispatchUser> users = request.getUserIds().stream()
                    .map(userId -> new DispatchUser(dispatch, userId))
                    .toList();
            dispatch.setDispatchUsers(users);
        }

        Dispatch savedDispatch = dispatchRepo.save(dispatch);

        processDispatch(savedDispatch);

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
            List<DispatchForm> forms = request.getFormIds().stream()
                    .map(formTreeNodeId -> new DispatchForm(dispatch, formTreeNodeId))
                    .toList();
            dispatch.getDispatchForms().forEach(form -> form.setStatus(dispatch.getStatus())); // Update status
            dispatch.getDispatchForms().clear();
            dispatch.getDispatchForms().addAll(forms);
        }

        // Handle DispatchUser
        if (request.getUserIds() != null) {
            List<DispatchUser> personnel = request.getUserIds().stream()
                    .map(userId -> new DispatchUser(dispatch, userId))
                    .toList();
            dispatch.getDispatchUsers().forEach(user -> user.setStatus(dispatch.getStatus())); // Update status
            dispatch.getDispatchUsers().clear();
            dispatch.getDispatchUsers().addAll(personnel);
        }

        Dispatch updatedDispatch = dispatchRepo.save(dispatch);

        // Update scheduled task
        if (Objects.equals(request.getType(), "SCHEDULED")) {
            taskScheduleService.scheduleDispatchTask(updatedDispatch, () -> executeDispatch(updatedDispatch.getId()));
        }

        return convertToDispatchDTO(updatedDispatch);
    }

    @Transactional
    public void deleteDispatch(Long id) {
        Dispatch dispatch = dispatchRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));

        // Cancel the dispatch task if scheduled
        boolean taskCancelled = taskScheduleService.cancelDispatch(id);
        if (taskCancelled) {
            logger.info("Cancelled scheduled task for Dispatch ID: {}", id);
        }
        dispatch.setUpdatedAt(OffsetDateTime.now());
        dispatch.setStatus(0);
        // Update the status of related personnel and forms
        if (dispatch.getDispatchUsers() != null) {
            dispatch.getDispatchUsers().forEach(personnel -> personnel.setStatus(0));
        }
        if (dispatch.getDispatchForms() != null) {
            dispatch.getDispatchForms().forEach(form -> form.setStatus(0));
        }

        dispatchRepo.save(dispatch);
        logger.info("Soft-deleted Dispatch ID: {}", id);
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

        // Convert to DTOs while filtering out tasks with non-existent dispatch references
        return dispatchedTasks.stream()
                .filter(dispatchedTask -> {
                    try {
                        // Check if Dispatch exists
                        return dispatchedTask.getDispatch() != null && dispatchedTask.getDispatch().getId() != null;
                    } catch (EntityNotFoundException e) {
                        // Log if the dispatch is missing
                        logger.warn("DispatchedTask with ID {} references a non-existent Dispatch.", dispatchedTask.getId());
                        return false; // Exclude from the result
                    }
                })
                .map(dispatchedTask -> {
                    DispatchedTaskDTO dto = modelMapper.map(dispatchedTask, DispatchedTaskDTO.class);

                    // Map nested properties
                    dto.setDispatchId(dispatchedTask.getDispatch().getId());
                    dto.setUserId(Long.valueOf(dispatchedTask.getUser().getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void scheduleDispatchTask(Long dispatchId, Runnable task) {
        Dispatch dispatch = dispatchRepo.findById(dispatchId)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));

        synchronized (dispatch.getId()) {
            OffsetDateTime now = OffsetDateTime.now();

            if (dispatch.getStartTime().isAfter(now)) {
                // If the start time is in the future, schedule a one-time task to re-evaluate at start time
                logger.info("Dispatch start time is in the future. Scheduling a one-time check at start time.");
                taskScheduleService.scheduleOneTimeTask(dispatch.getStartTime(), () -> {
                    logger.info("Re-evaluating dispatch ID {} at start time.", dispatchId);
                    this.scheduleDispatchTask(dispatchId, task);
                });
            } else if (dispatch.getEndTime().isBefore(now)) {
                // Log and skip scheduling as the end time is in the past
                logger.warn("Dispatch ID {} has an end time in the past. Skipping scheduling.", dispatchId);
            } else if (!taskScheduleService.isScheduled(dispatchId)) {
                // Schedule the task only if not already scheduled
                logger.info("Dispatch ID {} is active and within start/end time", dispatchId);
                taskScheduleService.scheduleDispatchTask(dispatch, task);
            } else {
                logger.info("Dispatch ID {} is already scheduled.", dispatchId);
            }
        }

    }

    @Override
    public void cancelDispatchTask(Long dispatchId) {
        Dispatch dispatch = dispatchRepo.findById(dispatchId)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));

        if (taskScheduleService.cancelDispatch(dispatchId)) {
            // Update status after cancellation
            dispatch.setUpdatedAt(OffsetDateTime.now());
            dispatchRepo.save(dispatch);
        } else {
            throw new IllegalStateException("No task was scheduled for this dispatch ID.");
        }
    }


    // HELPER CLASS ----------------------------------

    private void handleInactiveDispatch(Long dispatchId) {
        if (taskScheduleService.isScheduled(dispatchId)) {
            taskScheduleService.cancelDispatch(dispatchId);
        }
        logger.warn("Dispatch ID {} is inactive.", dispatchId);
    }

    private boolean hasReachedDispatchLimit(Dispatch dispatch) {
        return dispatch.getDispatchLimit() != -1 && dispatch.getExecutedCount() >= dispatch.getDispatchLimit();
    }

    private void handleDispatchLimitReached(Long dispatchId, Dispatch dispatch) {
        taskScheduleService.cancelDispatch(dispatchId);
        dispatch.setStatus(0); // Mark as inactive
        logger.info("Dispatch ID {} reached its limit and is now inactive.", dispatchId);
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

}