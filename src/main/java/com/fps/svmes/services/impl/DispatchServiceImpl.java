package com.fps.svmes.services.impl;

import com.fps.svmes.dto.requests.DispatchRequest;
import com.fps.svmes.models.sql.task_schedule.*;
import com.fps.svmes.repositories.jpaRepo.DispatchRepository;
import com.fps.svmes.repositories.jpaRepo.DispatchedTestRepository;
import com.fps.svmes.services.DispatchService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
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
    private DispatchedTestRepository testRepo;
    private static final Logger logger = LoggerFactory.getLogger(DispatchServiceImpl.class);

    // FOR WECOM NOTIFICATION LATER
    //    @Value("${wechat.api.url}")
    //    private String wechatApiUrl;
    //
    //    @Value("${wechat.access.token}")
    //    private String wechatAccessToken;
    //
    //    @Value("${wechat.template.id}")
    //    private String wechatTemplateId;

    // TEST DISPATCH SCHEDULING LOGIC --------------------------------------------------------------------------

    @Transactional
    @Override
    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    public void scheduleDispatches() {
        logger.info("Running scheduled dispatches check.");
        List<Dispatch> activeDispatches = dispatchRepo.findByActiveTrue();
        LocalDateTime now = LocalDateTime.now();

        for (Dispatch dispatch : activeDispatches) {
            if (shouldDispatch(dispatch, now)) {
                logger.info("Dispatch {} is scheduled for execution.", dispatch.getId());
                executeDispatch(dispatch.getId());
            } else {
                logger.debug("Dispatch {} skipped: Not eligible for execution at {}", dispatch.getId(), now);
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

        if (scheduleType == ScheduleType.SPECIFIC_DAYS) {
            String currentDay = now.getDayOfWeek().name();
            String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));

            // Check if specific days list is null or empty
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

        } else if (scheduleType == ScheduleType.INTERVAL) {
            if (dispatch.getIntervalMinutes() == null || dispatch.getStartTime() == null) {
                logger.warn("Dispatch {} has missing interval configuration.", dispatch.getId());
                return false;
            }

            if (dispatch.getRepeatCount() != null &&
                    dispatch.getExecutedCount() >= dispatch.getRepeatCount()) {
                logger.info("Dispatch {} already executed maximum times: {}", dispatch.getId(), dispatch.getRepeatCount());
                return false;
            }

            LocalDateTime nextDispatchTime = dispatch.getStartTime().plusMinutes(
                    (long) dispatch.getIntervalMinutes() * dispatch.getExecutedCount());
            boolean shouldDispatch = !now.isBefore(nextDispatchTime);
            logger.debug("Dispatch {} next execution time: {}, shouldDispatch result: {}", dispatch.getId(), nextDispatchTime, shouldDispatch);
            return shouldDispatch;
        }
        return false;
    }

    @Transactional
    @Override
    public void executeDispatch(Long dispatchId) {
        Dispatch dispatch = dispatchRepo.findById(dispatchId).orElseThrow();
        logger.info("Executing dispatch {}.", dispatchId);

        // Validate and fetch personnel list
        List<DispatchPersonnel> personnel = dispatch.getDispatchPersonnel();
        if (personnel == null || personnel.isEmpty()) {
            logger.warn("Dispatch {} skipped: Personnel list is null or empty.", dispatchId);
            return;
        }
        List<Integer> personnelList = personnel.stream()
                .map(dp -> dp.getUser().getId())
                .toList();

        // Validate and fetch form list
        List<DispatchForm> forms = dispatch.getDispatchForms();
        if (forms == null || forms.isEmpty()) {
            logger.warn("Dispatch {} skipped: Forms list is null or empty.", dispatchId);
            return;
        }
        List<Long> formIds = forms.stream()
                .map(DispatchForm::getFormId)
                .toList();

        // Determine dispatch time based on schedule type
        LocalDateTime calculatedDispatchTime;
        if (isIntervalSchedule(dispatch)) {
            calculatedDispatchTime = calculateDispatchTime(dispatch);
        } else {
            if (dispatch.getTimeOfDay() == null || dispatch.getTimeOfDay().trim().isEmpty()) {
                throw new IllegalStateException("Time of day is missing for SPECIFIC_DAYS schedule.");
            }
            calculatedDispatchTime = getSpecificDaysDispatchTime(dispatch);
        }
        logger.debug("Dispatch {} calculated dispatch time: {}", dispatchId, calculatedDispatchTime);

        // Create dispatched tests
        List<DispatchedTest> dispatchedTests = formIds.stream()
                .flatMap(formId -> personnelList.stream()
                        .map(personnelId -> createDispatchedTest(dispatch, formId, personnelId, calculatedDispatchTime)))
                .toList();

        // Batch save dispatched tests
        testRepo.saveAll(dispatchedTests);
        logger.info("Dispatch {} created {} tests.", dispatchId, dispatchedTests.size());

        // Send notifications
        dispatchedTests.forEach(test -> simulateNotification(
                test.getPersonnelId().intValue(),
                generateFormUrl(test.getPersonnelId().intValue(), test.getFormId())
        ));

        // Increment executed count only for INTERVAL schedule
        if (isIntervalSchedule(dispatch)) {
            incrementExecutedCount(dispatch);
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
    public Dispatch createDispatch(DispatchRequest request) {
        Dispatch dispatch = new Dispatch();

        // Base Dispatch Fields
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

            // Set specific days and time
            List<DispatchDay> days = request.getSpecificDays().stream()
                    .map(day -> new DispatchDay(dispatch, day))
                    .collect(Collectors.toList());
            dispatch.setDispatchDays(days);
            dispatch.setTimeOfDay(request.getTimeOfDay());

            // Nullify interval-specific fields
            dispatch.setIntervalMinutes(null);
            dispatch.setRepeatCount(null);
        }

        // Handle INTERVAL Schedule
        else if (request.getScheduleType() == DispatchRequest.ScheduleType.INTERVAL) {
            if (request.getIntervalMinutes() == null || request.getRepeatCount() == null) {
                throw new IllegalArgumentException("IntervalMinutes and RepeatCount must be provided for INTERVAL schedule");
            }

            // Set interval fields
            dispatch.setIntervalMinutes(request.getIntervalMinutes());
            dispatch.setRepeatCount(request.getRepeatCount());

            // Nullify specific-day fields
            dispatch.setDispatchDays(null);
            dispatch.setTimeOfDay(null);
        }

        // Handle DispatchForms
        if (request.getFormIds() != null) {
            List<DispatchForm> forms = request.getFormIds().stream()
                    .map(formId -> new DispatchForm(dispatch, formId))
                    .collect(Collectors.toList());
            dispatch.setDispatchForms(forms);
        }

        // Handle DispatchPersonnel
        if (request.getPersonnelIds() != null) {
            List<DispatchPersonnel> personnel = request.getPersonnelIds().stream()
                    .map(userId -> new DispatchPersonnel(dispatch, userId.intValue()))
                    .collect(Collectors.toList());
            dispatch.setDispatchPersonnel(personnel);
        }

        return dispatchRepo.save(dispatch);
    }

    @Transactional
    public Dispatch updateDispatch(Long id, DispatchRequest request) {
        // 1. Fetch the existing dispatch
        Dispatch dispatch = dispatchRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dispatch with ID " + id + " not found"));

        // 2. Update base Dispatch fields
        dispatch.setScheduleType(request.getScheduleType().name());
        dispatch.setActive(request.getActive());
        dispatch.setUpdatedAt(LocalDateTime.now());

        // 3. Update fields for SPECIFIC_DAYS schedule
        if (request.getScheduleType() == DispatchRequest.ScheduleType.SPECIFIC_DAYS) {
            if (request.getSpecificDays() == null || request.getTimeOfDay() == null) {
                throw new IllegalArgumentException("SpecificDays and TimeOfDay must be provided for SPECIFIC_DAYS schedule");
            }

            // Set timeOfDay and clear/re-add specific days
            dispatch.setTimeOfDay(request.getTimeOfDay());
            dispatch.getDispatchDays().clear();
            List<DispatchDay> days = request.getSpecificDays().stream()
                    .map(day -> new DispatchDay(dispatch, day))
                    .toList();
            dispatch.getDispatchDays().addAll(days);

            // Nullify interval fields
            dispatch.setIntervalMinutes(null);
            dispatch.setRepeatCount(null);
        }
        // 4. Update fields for INTERVAL schedule
        else if (request.getScheduleType() == DispatchRequest.ScheduleType.INTERVAL) {
            if (request.getIntervalMinutes() == null || request.getRepeatCount() == null) {
                throw new IllegalArgumentException("IntervalMinutes and RepeatCount must be provided for INTERVAL schedule");
            }

            // Set interval fields and clear specific days
            dispatch.setIntervalMinutes(request.getIntervalMinutes());
            dispatch.setRepeatCount(request.getRepeatCount());
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
        return dispatchRepo.save(dispatch);
    }


    /**
     * Fetch a single dispatch by its ID.
     * @param id The ID of the dispatch to fetch.
     * @return The Dispatch entity with all related entities (days, forms, personnel).
     */
    public Dispatch getDispatch(Long id) {
        return dispatchRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch with ID " + id + " not found"));
    }

    /**
     * Fetch all dispatches with their relationships.
     * @return A list of all Dispatch entities.
     */
    public List<Dispatch> getAllDispatches() {
        return dispatchRepo.findAll();
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

    private void saveRelatedEntities(Dispatch savedDispatch, Dispatch originalDispatch) {
        // Save middle table relationships for DispatchDays
        if (originalDispatch.getDispatchDays() != null) {
            savedDispatch.setDispatchDays(originalDispatch.getDispatchDays()
                    .stream()
                    .map(day -> new DispatchDay(savedDispatch, day.getDay()))
                    .collect(Collectors.toList()));
        } else {
            savedDispatch.setDispatchDays(List.of());
        }

        // Save middle table relationships for DispatchForms
        if (originalDispatch.getDispatchForms() != null) {
            savedDispatch.setDispatchForms(originalDispatch.getDispatchForms()
                    .stream()
                    .map(form -> new DispatchForm(savedDispatch, form.getFormId()))
                    .collect(Collectors.toList()));
        } else {
            savedDispatch.setDispatchForms(List.of());
        }

        // Save middle table relationships for DispatchPersonnel
        if (originalDispatch.getDispatchPersonnel() != null) {
            savedDispatch.setDispatchPersonnel(originalDispatch.getDispatchPersonnel()
                    .stream()
                    .map(personnel -> new DispatchPersonnel(savedDispatch, personnel.getUser().getId()))
                    .collect(Collectors.toList()));
        } else {
            savedDispatch.setDispatchPersonnel(List.of());
        }
    }

    private LocalDateTime getSpecificDaysDispatchTime(Dispatch dispatch) {
        if (dispatch.getTimeOfDay() == null || dispatch.getTimeOfDay().isEmpty()) {
            throw new IllegalStateException("Time of day is missing for SPECIFIC_DAYS schedule.");
        }

        LocalDateTime now = LocalDateTime.now();
        String[] timeParts = dispatch.getTimeOfDay().split(":");

        return now.withHour(Integer.parseInt(timeParts[0]))
                .withMinute(Integer.parseInt(timeParts[1]))
                .withSecond(0)
                .withNano(0);
    }

    private boolean isIntervalSchedule(Dispatch dispatch) {
        return ScheduleType.INTERVAL.name().equals(dispatch.getScheduleType());
    }

    private LocalDateTime calculateDispatchTime(Dispatch dispatch) {
        if (dispatch.getStartTime() == null || dispatch.getIntervalMinutes() == null) {
            throw new IllegalStateException("Invalid INTERVAL configuration: Missing start time or interval minutes.");
        }
        int nextExecutedCount = dispatch.getExecutedCount() + 1; // Use the next count
        return dispatch.getStartTime().plusMinutes((long) dispatch.getIntervalMinutes() * nextExecutedCount);
    }

    // Create a single DispatchedTest object
    private DispatchedTest createDispatchedTest(Dispatch dispatch, Long formId, Integer personnelId, LocalDateTime dispatchTime) {
        DispatchedTest test = new DispatchedTest();
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

    // FOR LATER WECOM
    //    /**
    //     * Sends a WeChat notification to the personnel with the form URL.
    //     *
    //     * @param personnelId the ID of the personnel
    //     * @param formUrl the URL of the form
    //     */
    //    private void notifyPersonnel(Long personnelId, String formUrl) {
    //        String url = wechatApiUrl + "/cgi-bin/message/template/send?access_token=" + wechatAccessToken;
    //        RestTemplate restTemplate = new RestTemplate();
    //
    //        var payload = new java.util.HashMap<String, Object>();
    //        payload.put("touser", "wechat-id-for-personnel-" + personnelId); // Replace with actual retrieval of WeChat ID
    //
    //        payload.put("template_id", wechatTemplateId);
    //        payload.put("url", formUrl);
    //        payload.put("data", java.util.Map.of(
    //                "first", java.util.Map.of("value", "You have a new QC test assignment."),
    //                "keyword1", java.util.Map.of("value", "Form Link"),
    //                "keyword2", java.util.Map.of("value", "High Priority"),
    //                "remark", java.util.Map.of("value", "Please complete it as soon as possible.")
    //        ));
    //
    //        try {
    //            restTemplate.postForObject(url, payload, String.class);
    //            System.out.println("WeChat notification sent for Personnel ID: " + personnelId);
    //        } catch (Exception e) {
    //            System.err.println("Failed to send WeChat notification: " + e.getMessage());
    //        }
    //    }
}