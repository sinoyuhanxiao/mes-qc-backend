package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
import com.fps.svmes.dto.dtos.dispatch.DispatchedTaskDTO;
import com.fps.svmes.dto.requests.DispatchRequest;
import com.fps.svmes.models.sql.task_schedule.*;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchRepository;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchedTestRepository;
import com.fps.svmes.services.DispatchService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
    private static final Logger logger = LoggerFactory.getLogger(DispatchServiceImpl.class);

    // TEST DISPATCH SCHEDULING LOGIC --------------------------------------------------------------------------

    @Transactional
    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    @Override
    public synchronized void scheduleDispatches() {
        logger.info("Running scheduled dispatches check.");
        List<Dispatch> activeDispatches = dispatchRepo.findByActiveTrue();
        LocalDateTime now = LocalDateTime.now();

        for (Dispatch dispatch : activeDispatches) {
            try {
                if (shouldDispatch(dispatch, now)) {
                    logger.info("Dispatch {} is scheduled for execution.", dispatch.getId());
                    executeDispatch(dispatch.getId());
                } else {
                    logger.debug("Dispatch {} skipped: Not eligible for execution at {}", dispatch.getId(), now);
                }
            } catch (IllegalStateException e) {
                logger.warn("Skipping dispatch {} due to configuration issue: {}", dispatch.getId(), e.getMessage());
            } catch (Exception e) {
                logger.error("Error processing dispatch {}: {}", dispatch.getId(), e.getMessage(), e);
            }
        }
    }

    public boolean shouldDispatch(Dispatch dispatch, LocalDateTime now) {
        ScheduleType scheduleType;
        try {
            scheduleType = ScheduleType.valueOf(dispatch.getScheduleType());
        } catch (IllegalArgumentException | NullPointerException e) {
            logger.warn("Invalid or null schedule type for dispatch {}: {}", dispatch.getId(), e.getMessage());
            return false;
        }

        switch (scheduleType) {
            case SPECIFIC_DAYS -> {
                return checkSpecificDaysSchedule(dispatch, now);
            }
            case INTERVAL -> {
                return checkIntervalSchedule(dispatch, now);
            }
            default -> {
                logger.warn("Unsupported schedule type for dispatch {}: {}", dispatch.getId(), scheduleType);
                return false;
            }
        }
    }

    private boolean checkSpecificDaysSchedule(Dispatch dispatch, LocalDateTime now) {
        String currentDay = now.getDayOfWeek().name();
        String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        List<DispatchDay> specificDays = dispatch.getDispatchDays();
        if (specificDays == null || specificDays.isEmpty()) {
            logger.warn("Dispatch {} has no specific days configured.", dispatch.getId());
            return false;
        }

        boolean shouldDispatch = specificDays.stream()
                .anyMatch(day -> day.getDay().equalsIgnoreCase(currentDay)) &&
                currentTime.equals(dispatch.getTimeOfDay());

        logger.debug("Dispatch {} shouldDispatch result: {}", dispatch.getId(), shouldDispatch);
        return shouldDispatch;
    }

    private boolean checkIntervalSchedule(Dispatch dispatch, LocalDateTime now) {
        if (dispatch.getIntervalMinutes() == null || dispatch.getStartTime() == null) {
            logger.warn("Dispatch {} has missing interval configuration.", dispatch.getId());
            return false;
        }

        if (dispatch.getRepeatCount() != null &&
                dispatch.getExecutedCount() >= dispatch.getRepeatCount()) {
            // Deactivate the dispatch if it has reached its max executions
            dispatch.setActive(false);
            dispatch.setUpdatedAt(LocalDateTime.now());
            dispatchRepo.save(dispatch);
            logger.info("Dispatch {} deactivated: Executed maximum times.", dispatch.getId());
            return false;
        }

        LocalDateTime nextDispatchTime = dispatch.getStartTime().plusMinutes(
                (long) dispatch.getIntervalMinutes() * dispatch.getExecutedCount());
        boolean shouldDispatch = !now.isBefore(nextDispatchTime);

        logger.debug("Dispatch {} next execution time: {}, shouldDispatch result: {}", dispatch.getId(), nextDispatchTime, shouldDispatch);
        return shouldDispatch;
    }

    @Transactional
    @Override
    public void executeDispatch(Long dispatchId) {
        Dispatch dispatch = dispatchRepo.findById(dispatchId).orElseThrow();
        logger.info("Executing dispatch {}.", dispatchId);

        try {
            // Validate interval-specific fields
            if (isIntervalSchedule(dispatch)) {
                if (dispatch.getStartTime() == null || dispatch.getIntervalMinutes() == null) {
                    throw new IllegalStateException("Invalid INTERVAL configuration: Missing start time or interval minutes.");
                }
            }

            // Validate and fetch personnel and forms
            List<Integer> personnelList = validateAndGetPersonnel(dispatch, dispatchId);
            List<Long> formIds = validateAndGetForms(dispatch, dispatchId);

            // Simulate incremented count for calculation but avoid persistence yet
            int simulatedExecutedCount = dispatch.getExecutedCount();
            if (isIntervalSchedule(dispatch)) {
                simulatedExecutedCount++; // Simulate increment for dispatch calculation
            }

            // Determine dispatch time
            LocalDateTime calculatedDispatchTime = calculateDispatchTime(dispatch, simulatedExecutedCount);

            logger.debug("Dispatch {} calculated dispatch time: {}", dispatchId, calculatedDispatchTime);

            // Create dispatched tests
            List<DispatchedTask> dispatchedTasks = formIds.stream()
                    .flatMap(formId -> personnelList.stream()
                            .map(personnelId -> createDispatchedTest(dispatch, formId, personnelId, calculatedDispatchTime)))
                    .toList();

            // Save dispatched tests
            dispatchedTaskRepo.saveAll(dispatchedTasks);
            logger.info("Dispatch {} created {} tests.", dispatchId, dispatchedTasks.size());

            // Send notifications
            dispatchedTasks.forEach(test -> simulateNotification(
                    test.getPersonnelId().intValue(),
                    generateFormUrl(test.getPersonnelId().intValue(), test.getFormId())
            ));

            // Persist incremented executed count
            if (isIntervalSchedule(dispatch)) {
                incrementExecutedCount(dispatch); // Increment persisted value
            }
        } catch (IllegalStateException e) {
            logger.warn("Skipping execution of dispatch {}: {}", dispatchId, e.getMessage());
        }
    }

    @Override
    public boolean manualDispatch(Long id) {
        if (dispatchRepo.existsById(id)) {
            executeDispatch(id);
            return true;
        }
        return false;
    }

    // DISPATCH CRUD LOGIC --------------------------------------------------------------------------

    @Transactional
    public DispatchDTO createDispatch(DispatchRequest request) {
        Dispatch dispatch = new Dispatch();

        // Base Dispatch Fields
        dispatch.setName(request.getName());
        dispatch.setScheduleType(request.getScheduleType().name());
        dispatch.setActive(request.getActive());
        dispatch.setCreatedAt(LocalDateTime.now());
        dispatch.setUpdatedAt(LocalDateTime.now());
        dispatch.setExecutedCount(0);

        // Handle SPECIFIC_DAYS Schedule
        if (request.getScheduleType() == DispatchRequest.ScheduleType.SPECIFIC_DAYS) {
            if (request.getSpecificDays() == null || request.getTimeOfDay() == null) {
                throw new IllegalArgumentException("SpecificDays and TimeOfDay must be provided for SPECIFIC_DAYS schedule");
            }

            List<DispatchDay> days = request.getSpecificDays().stream()
                    .map(day -> new DispatchDay(dispatch, day))
                    .collect(Collectors.toList());
            dispatch.setDispatchDays(days);
            dispatch.setTimeOfDay(request.getTimeOfDay());
            dispatch.setIntervalMinutes(null);
            dispatch.setRepeatCount(null);
        }

        // Handle INTERVAL Schedule
        else if (request.getScheduleType() == DispatchRequest.ScheduleType.INTERVAL) {
            if (request.getIntervalMinutes() == null || request.getRepeatCount() == null) {
                throw new IllegalArgumentException("IntervalMinutes and RepeatCount must be provided for INTERVAL schedule");
            }
            dispatch.setIntervalMinutes(request.getIntervalMinutes());
            dispatch.setRepeatCount(request.getRepeatCount());
            dispatch.setStartTime(request.getStartTime());
            dispatch.setDispatchDays(null);
            dispatch.setTimeOfDay(null);
        }

        // Handle DispatchForms
        if (request.getFormIds() != null) {
            List<DispatchForm> forms = request.getFormIds().stream()
                    .map(formId -> new DispatchForm(dispatch, formId))
                    .toList();
            dispatch.setDispatchForms(forms);
        }

        // Handle DispatchPersonnel
        if (request.getPersonnelIds() != null) {
            List<DispatchPersonnel> personnel = request.getPersonnelIds().stream()
                    .map(userId -> new DispatchPersonnel(dispatch, userId.intValue()))
                    .toList();
            dispatch.setDispatchPersonnel(personnel);
        }

        // Save entity and return DTO
        Dispatch savedDispatch = dispatchRepo.save(dispatch);
        return convertToDTO(savedDispatch);
    }

    @Transactional
    public DispatchDTO updateDispatch(Long id, DispatchRequest request) {
        // 1. Fetch the existing dispatch
        Dispatch dispatch = dispatchRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dispatch with ID " + id + " not found"));

        // 2. Update base Dispatch fields
        dispatch.setName(request.getName());
        dispatch.setScheduleType(request.getScheduleType().name());
        dispatch.setActive(request.getActive());
        dispatch.setUpdatedAt(LocalDateTime.now());

        // 3. Update fields for SPECIFIC_DAYS schedule
        if (request.getScheduleType() == DispatchRequest.ScheduleType.SPECIFIC_DAYS) {
            if (request.getSpecificDays() == null || request.getTimeOfDay() == null) {
                throw new IllegalArgumentException("SpecificDays and TimeOfDay must be provided for SPECIFIC_DAYS schedule");
            }

            // Update specificDays and timeOfDay
            dispatch.setTimeOfDay(request.getTimeOfDay());
            dispatch.getDispatchDays().clear();
            List<DispatchDay> days = request.getSpecificDays().stream()
                    .map(day -> new DispatchDay(dispatch, day))
                    .toList();
            dispatch.getDispatchDays().addAll(days);

            // Reset irrelevant fields
            dispatch.setIntervalMinutes(null);
            dispatch.setRepeatCount(null);
            dispatch.setExecutedCount(0);
        }
        // 4. Update fields for INTERVAL schedule
        else if (request.getScheduleType() == DispatchRequest.ScheduleType.INTERVAL) {
            if (request.getIntervalMinutes() == null || request.getRepeatCount() == null) {
                throw new IllegalArgumentException("IntervalMinutes and RepeatCount must be provided for INTERVAL schedule");
            }

            // Set interval fields
            dispatch.setIntervalMinutes(request.getIntervalMinutes());
            dispatch.setRepeatCount(request.getRepeatCount());
            dispatch.setExecutedCount(0); // Optionally reset executed count if interval is changed

            // Reset irrelevant fields
            dispatch.setTimeOfDay(null);
            dispatch.getDispatchDays().clear();
        }

        // 5. Update DispatchForm relationships
        dispatch.getDispatchForms().clear();
        if (request.getFormIds() != null) {
            List<DispatchForm> forms = request.getFormIds().stream()
                    .map(formId -> new DispatchForm(dispatch, formId))
                    .toList();
            dispatch.getDispatchForms().addAll(forms);
        }

        // 6. Update DispatchPersonnel relationships
        dispatch.getDispatchPersonnel().clear();
        if (request.getPersonnelIds() != null) {
            List<DispatchPersonnel> personnel = request.getPersonnelIds().stream()
                    .map(userId -> new DispatchPersonnel(dispatch, userId.intValue()))
                    .toList();
            dispatch.getDispatchPersonnel().addAll(personnel);
        }

        // 7. Save and return the updated dispatch
        Dispatch updatedDispatch = dispatchRepo.save(dispatch);
        return convertToDTO(updatedDispatch);
    }


    /**
     * Fetch a single dispatch by its ID.
     * @param id The ID of the dispatch to fetch.
     * @return The Dispatch entity with all related entities (days, forms, personnel).
     */
    @Transactional(readOnly = true)
    public DispatchDTO getDispatch(Long id) {
        Dispatch dispatch = dispatchRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch with ID " + id + " not found"));

        DispatchDTO dto = new DispatchDTO();
        dto.setId(dispatch.getId());
        dto.setName(dispatch.getName());
        dto.setScheduleType(dispatch.getScheduleType());
        dto.setTimeOfDay(dispatch.getTimeOfDay());
        dto.setIntervalMinutes(dispatch.getIntervalMinutes());
        dto.setRepeatCount(dispatch.getRepeatCount());
        dto.setStartTime(dispatch.getStartTime()); // Set start_time
        dto.setExecutedCount(dispatch.getExecutedCount()); // Set executed_count
        dto.setActive(dispatch.getActive()); // Set active flag
        dto.setCreatedAt(dispatch.getCreatedAt()); // Set created_at
        dto.setUpdatedAt(dispatch.getUpdatedAt()); // Set updated_at

        // Map related entities to DTO
        dto.setDispatchDays(dispatch.getDispatchDays().stream()
                .map(DispatchDay::getDay).toList());
        dto.setFormIds(dispatch.getDispatchForms().stream()
                .map(DispatchForm::getFormId).toList());
        dto.setPersonnelIds(dispatch.getDispatchPersonnel().stream()
                .map(personnel -> Long.valueOf(personnel.getUser().getId())).toList());

        return dto;
    }


    @Transactional(readOnly = true)
    public List<DispatchDTO> getAllDispatches() {
        return dispatchRepo.findAll().stream().map(dispatch -> {
            DispatchDTO dto = new DispatchDTO();

            // Base fields
            dto.setId(dispatch.getId());
            dto.setName(dispatch.getName());
            dto.setScheduleType(dispatch.getScheduleType());
            dto.setTimeOfDay(dispatch.getTimeOfDay());
            dto.setIntervalMinutes(dispatch.getIntervalMinutes());
            dto.setRepeatCount(dispatch.getRepeatCount());
            dto.setStartTime(dispatch.getStartTime());
            dto.setExecutedCount(dispatch.getExecutedCount());
            dto.setActive(dispatch.getActive());
            dto.setCreatedAt(dispatch.getCreatedAt());
            dto.setUpdatedAt(dispatch.getUpdatedAt());

            // Map related entities
            dto.setDispatchDays(dispatch.getDispatchDays().stream()
                    .map(DispatchDay::getDay)
                    .collect(Collectors.toList()));

            dto.setFormIds(dispatch.getDispatchForms().stream()
                    .map(DispatchForm::getFormId)
                    .collect(Collectors.toList()));

            dto.setPersonnelIds(dispatch.getDispatchPersonnel().stream()
                    .map(personnel -> Long.valueOf(personnel.getUser().getId()))
                    .collect(Collectors.toList()));

            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DispatchedTaskDTO> getAllDispatchedTasks() {
        return dispatchedTaskRepo.findAll().stream().map(dispatchedTask -> {
            DispatchedTaskDTO dto = new DispatchedTaskDTO();

            // Base fields
            dto.setId(dispatchedTask.getId());
            dto.setDispatchId(dispatchedTask.getId());
            dto.setPersonnelId(dispatchedTask.getPersonnelId());
            dto.setFormId(dispatchedTask.getFormId());
            dto.setDispatchTime(dispatchedTask.getDispatchTime());
            dto.setStatus(dispatchedTask.getStatus());
            dto.setNotes(dispatchedTask.getNotes());
            dto.setLastUpdated(dispatchedTask.getLastUpdated());
            dto.setFinishedAt(dispatchedTask.getFinishedAt());

            return dto;
        }).collect(Collectors.toList());
    }


    /**
     * Delete a dispatch by its ID.
     * @param id The ID of the dispatch to delete.
     */
    @Transactional
    public void deleteDispatch(Long id) {
        // Fetch the existing dispatch
        Dispatch existingDispatch = dispatchRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch with ID " + id + " not found"));

        // Delete the dispatch
        dispatchRepo.delete(existingDispatch);
    }



    // HELPER METHODS --------------------------------------------------------------------------

    private DispatchDTO convertToDTO(Dispatch dispatch) {
        DispatchDTO dto = new DispatchDTO();
        dto.setId(dispatch.getId());
        dto.setName(dispatch.getName());
        dto.setScheduleType(dispatch.getScheduleType());
        dto.setIntervalMinutes(dispatch.getIntervalMinutes());
        dto.setRepeatCount(dispatch.getRepeatCount());
        dto.setStartTime(dispatch.getStartTime());
        dto.setTimeOfDay(dispatch.getTimeOfDay());
        dto.setActive(dispatch.getActive());

        // Convert DispatchDays to specificDays
        if (dispatch.getDispatchDays() != null) {
            dto.setDispatchDays(dispatch.getDispatchDays().stream()
                    .map(DispatchDay::getDay)
                    .toList());
        }

        // Convert DispatchForms to formIds
        if (dispatch.getDispatchForms() != null) {
            dto.setFormIds(dispatch.getDispatchForms().stream()
                    .map(DispatchForm::getFormId)
                    .toList());
        }

        // Convert DispatchPersonnel to personnelIds
        if (dispatch.getDispatchPersonnel() != null) {
            dto.setPersonnelIds(dispatch.getDispatchPersonnel().stream()
                    .map(personnel -> Long.valueOf(personnel.getUser().getId()))
                    .toList());
        }

        return dto;
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

    private List<Long> validateAndGetForms(Dispatch dispatch, Long dispatchId) {
        List<DispatchForm> forms = dispatch.getDispatchForms();
        if (forms == null || forms.isEmpty()) {
            logger.warn("Dispatch {} skipped: Forms list is null or empty.", dispatchId);
            throw new IllegalStateException("Forms list is required.");
        }
        return forms.stream()
                .map(DispatchForm::getFormId)
                .toList();
    }

    private LocalDateTime calculateDispatchTime(Dispatch dispatch, int simulatedExecutedCount) {
        if (isIntervalSchedule(dispatch)) {
            if (dispatch.getStartTime() == null || dispatch.getIntervalMinutes() == null) {
                throw new IllegalStateException("Invalid INTERVAL configuration: Missing start time or interval minutes.");
            }
            return dispatch.getStartTime().plusMinutes(
                    (long) dispatch.getIntervalMinutes() * simulatedExecutedCount);
        } else {
            if (dispatch.getTimeOfDay() == null || dispatch.getTimeOfDay().trim().isEmpty()) {
                throw new IllegalStateException("Time of day is missing for SPECIFIC_DAYS schedule.");
            }
            return LocalDateTime.now().with(LocalTime.parse(dispatch.getTimeOfDay()));
        }
    }


    private boolean isIntervalSchedule(Dispatch dispatch) {
        return ScheduleType.INTERVAL.name().equals(dispatch.getScheduleType());
    }


    // Create a single DispatchedTest object
    private DispatchedTask createDispatchedTest(Dispatch dispatch, Long formId, Integer personnelId, LocalDateTime dispatchTime) {
        DispatchedTask test = new DispatchedTask();
        test.setDispatch(dispatch);
        test.setFormId(formId);
        test.setPersonnelId(Long.valueOf(personnelId));
        test.setDispatchTime(dispatchTime);
        test.setStatus("PENDING");
        return test;
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
     * @param personnelId the ID of the personnel
     * @return the generated URL
     */
    private String generateFormUrl(int personnelId, Long formId) {
        return "https://your-system.com/forms/" + formId + "?user=" + personnelId;
    }

    /**
     * Simulates sending a notification by printing to the console.
     *
     * @param personnelId the ID of the personnel
     * @param formUrl the URL of the form
     */
    private void simulateNotification(int personnelId, String formUrl) {
        logger.info("Simulating notification to Personnel ID: {} with Form URL: {}", personnelId, formUrl);
    }

}