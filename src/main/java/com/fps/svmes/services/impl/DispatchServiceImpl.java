package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
import com.fps.svmes.dto.dtos.dispatch.DispatchedTaskDTO;
import com.fps.svmes.dto.dtos.user.UserDTO;
import com.fps.svmes.dto.requests.DispatchRequest;
import com.fps.svmes.models.sql.task_schedule.*;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchRepository;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchedTestRepository;
import com.fps.svmes.services.DispatchService;
import com.fps.svmes.services.TaskScheduleService;
import com.fps.svmes.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.modelmapper.ModelMapper;

import java.sql.Timestamp;
import java.util.List;
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
    private DispatchedTestRepository dispatchedTaskRepo;

    @Autowired
    private TaskScheduleService taskScheduleService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(DispatchServiceImpl.class);

    // TEST DISPATCH SCHEDULING LOGIC --------------------------------------------------------------------------

    @Override
    public void scheduleDispatches() {
        List<Dispatch> activeDispatches = dispatchRepo.findByStatus(1); // Fetch active dispatches
        for (Dispatch dispatch : activeDispatches) {
            try {
                taskScheduleService.scheduleDispatchTask(dispatch, () -> executeDispatch(dispatch.getId()));
                logger.info("Scheduled task for Dispatch ID: {}", dispatch.getId());
            } catch (Exception e) {
                logger.error("Failed to schedule task for Dispatch ID: {}", dispatch.getId(), e);
            }
        }
    }


    @Transactional
    @Override
    public void executeDispatch(Long dispatchId) {
        Dispatch dispatch = this.getDispatchWithDetails(dispatchId);

        if (dispatch.getStatus() == 0) {
            logger.warn("Dispatch ID {} is inactive.", dispatchId);
            return;
        }

        try {
            List<DispatchedTask> dispatchedTasks = createTasksForDispatch(dispatch);
            dispatchedTaskRepo.saveAll(dispatchedTasks);

            // Increment executed count
            dispatch.setExecutedCount(dispatch.getExecutedCount() + 1);
            if (dispatch.getDispatchLimit() != -1 && dispatch.getExecutedCount() >= dispatch.getDispatchLimit()) {
                dispatch.setStatus(0); // Mark as inactive
                logger.info("Dispatch ID {} reached its limit and is now inactive.", dispatchId);
            }
            dispatchRepo.save(dispatch);

            logger.info("Executed Dispatch ID: {}, Created {} tasks.", dispatchId, dispatchedTasks.size());
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

        // Handle DispatchPersonnel
        if (request.getUserIds() != null) {
            List<DispatchPersonnel> personnel = request.getUserIds().stream()
                    .map(userId -> new DispatchPersonnel(dispatch, userId))
                    .toList();
            dispatch.setDispatchPersonnel(personnel);
        }

        Dispatch savedDispatch = dispatchRepo.save(dispatch);

        this.scheduleDispatchTask(savedDispatch.getId(), () -> executeDispatch(savedDispatch.getId()));
        // Schedule the task

        return convertToDispatchDTO(savedDispatch);
    }

    @Transactional
    public DispatchDTO updateDispatch(Long id, DispatchRequest request) {
        Dispatch dispatch = dispatchRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));

        modelMapper.map(request, dispatch);
        dispatch.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        // Handle DispatchForms
        if (request.getFormIds() != null) {
            List<DispatchForm> forms = request.getFormIds().stream()
                    .map(formTreeNodeId -> new DispatchForm(dispatch, formTreeNodeId))
                    .toList();
            dispatch.getDispatchForms().clear();
            dispatch.getDispatchForms().addAll(forms);
        }

        // Handle DispatchPersonnel
        if (request.getUserIds() != null) {
            List<DispatchPersonnel> personnel = request.getUserIds().stream()
                    .map(userId -> new DispatchPersonnel(dispatch, userId))
                    .toList();
            dispatch.getDispatchPersonnel().clear();
            dispatch.getDispatchPersonnel().addAll(personnel);
        }

        Dispatch updatedDispatch = dispatchRepo.save(dispatch);

        // Update scheduled task
        taskScheduleService.scheduleDispatchTask(updatedDispatch, () -> executeDispatch(updatedDispatch.getId()));

        return convertToDispatchDTO(updatedDispatch);
    }



    @Transactional
    public void deleteDispatch(Long id) {
        if (dispatchRepo.existsById(id)) {
            taskScheduleService.cancelDispatch(id);
            dispatchRepo.deleteById(id);
            logger.info("Deleted Dispatch ID: {}", id);
        } else {
            throw new EntityNotFoundException("Dispatch not found");
        }
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

        // Convert to DTOs
        return dispatchedTasks.stream()
                .map(dispatchedTask -> {
                    DispatchedTaskDTO dto = modelMapper.map(dispatchedTask, DispatchedTaskDTO.class);

                    // Map nested properties
                    dto.setDispatchId(dispatchedTask.getDispatch().getId());
                    dto.setUser(userService.getUserById(dispatchedTask.getUser().getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }
    @Override
    public void scheduleDispatchTask(Long dispatchId, Runnable task) {
        Dispatch dispatch = dispatchRepo.findById(dispatchId)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));

        if (dispatch.isActiveAndWithinScheduledTime()) {
            logger.info("Dispatch is active and within start/end time");
            // Schedule the task
            taskScheduleService.scheduleDispatchTask(dispatch, task);
        } else {
            throw new IllegalStateException("Dispatch is not active or within the scheduled time.");
        }
    }

    @Override
    public void cancelDispatchTask(Long dispatchId) {
        Dispatch dispatch = dispatchRepo.findById(dispatchId)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));

        if (taskScheduleService.cancelDispatch(dispatchId)) {
            // Update status after cancellation
            dispatch.setStatus(0); // Mark as inactive
            dispatch.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            dispatchRepo.save(dispatch);
        } else {
            throw new IllegalStateException("No task was scheduled for this dispatch ID.");
        }
    }


    // HELPER CLASS ----------------------------------


    @Transactional
    public Dispatch getDispatchWithDetails(Long id) {
        Dispatch dispatch = dispatchRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));
        dispatchRepo.findByIdWithForms(id).ifPresent(fetched -> dispatch.setDispatchForms(fetched.getDispatchForms()));
        dispatchRepo.findByIdWithPersonnel(id).ifPresent(fetched -> dispatch.setDispatchPersonnel(fetched.getDispatchPersonnel()));
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
        if (dispatch.getDispatchPersonnel() != null) {
            dto.setPersonnel(dispatch.getDispatchPersonnel().stream()
                    .map(personnel -> modelMapper.map(personnel.getUser(), UserDTO.class))
                    .toList());
        }

        return dto;
    }


    private List<DispatchedTask> createTasksForDispatch(Dispatch dispatch) {
        Timestamp dispatchTime = new Timestamp(System.currentTimeMillis());
        Timestamp dueDate = calculateDueDate(dispatchTime, dispatch.getDueDateOffsetMinute());

        // Logic to create DispatchedTask entities based on dispatch forms and personnel
        return dispatch.getDispatchForms().stream()
                .flatMap(form -> dispatch.getDispatchPersonnel().stream()
                        .map(personnel -> {
                            DispatchedTask task = new DispatchedTask();
                            task.setDispatch(dispatch);
                            task.setName(dispatch.getName());
                            task.setUser(personnel.getUser());
                            task.setQcFormTreeNodeId(form.getQcFormTreeNodeId());
                            task.setDispatchTime(dispatchTime);
                            task.setNotes(dispatch.getRemark());
                            task.setDescription(dispatch.getRemark());
                            task.setCreationDetails(30, 1);
                            task.setDueDate(dueDate);
                            task.setOverdue(false);
                            task.setDispatchedTaskStateId(1); // Default state ID
                            task.setStatus(1); // Active
                            return task;
                        }))
                .collect(Collectors.toList());
    }

    private Timestamp calculateDueDate(Timestamp dispatchTime, int dueDateOffsetMinute) {
        long offsetMillis = dueDateOffsetMinute * 60 * 1000L; // Convert minutes to milliseconds
        return new Timestamp(dispatchTime.getTime() + offsetMillis);
    }


    private List<Integer> validateAndGetPersonnel(Dispatch dispatch, Long dispatchId) {
        List<DispatchPersonnel> personnel = dispatch.getDispatchPersonnel();
        if (personnel == null || personnel.isEmpty()) {
            logger.warn("Dispatch {} skipped: Personnel list is null or empty.", dispatchId);
            throw new IllegalStateException("Personnel list is required.");
        }
        return personnel.stream()
                .map(dp -> dp.getUser().getId())
                .toList();
    }

    private List<String> validateAndGetForms(Dispatch dispatch, Long dispatchId) {
        List<DispatchForm> forms = dispatch.getDispatchForms();
        if (forms == null || forms.isEmpty()) {
            logger.warn("Dispatch {} skipped: Forms list is null or empty.", dispatchId);
            throw new IllegalStateException("Forms list is required.");
        }
        return forms.stream()
                .map(DispatchForm::getQcFormTreeNodeId)
                .toList();
    }


    // Increment executed count and save the dispatch
    private void incrementExecutedCount(Dispatch dispatch) {
        dispatch.setExecutedCount(dispatch.getExecutedCount() + 1);
        dispatchRepo.save(dispatch);
        logger.info("Dispatch {} executed count incremented to {}", dispatch.getId(), dispatch.getExecutedCount());
    }

    /**
     * Generates a unique URL for a form assigned to a personnel.
     *
     * @param formId the ID of the form
     * @param userId the ID of the personnel
     * @return the generated URL
     */
    private String generateFormUrl(int userId, String formId) {
        return "https://your-system.com/forms/" + formId + "?user=" + userId;
    }

    /**
     * Simulates sending a notification by printing to the console.
     *
     * @param userId the ID of the personnel
     * @param formUrl the URL of the form
     */
    private void simulateNotification(int userId, String formUrl) {
        logger.info("Simulating notification to Personnel ID: {} with Form URL: {}", userId, formUrl);
    }


}