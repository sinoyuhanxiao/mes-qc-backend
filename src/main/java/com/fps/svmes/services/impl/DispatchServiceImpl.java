package com.fps.svmes.services.impl;

import com.fps.svmes.models.sql.task_schedule.*;
import com.fps.svmes.repositories.jpaRepo.DispatchRepository;
import com.fps.svmes.repositories.jpaRepo.DispatchedTestRepository;
import com.fps.svmes.services.DispatchService;
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
    // FOR WECOM LATER
//    @Value("${wechat.api.url}")
//    private String wechatApiUrl;
//
//    @Value("${wechat.access.token}")
//    private String wechatAccessToken;
//
//    @Value("${wechat.template.id}")
//    private String wechatTemplateId;

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
    public Dispatch createDispatch(Dispatch dispatch) {
        // Save the Dispatch entity and related middle-table entities
        Dispatch savedDispatch = dispatchRepo.save(dispatch);
        saveRelatedEntities(savedDispatch, dispatch);
        return dispatchRepo.save(savedDispatch);
    }

    @Override
    public Optional<Dispatch> getDispatchById(Long id) {
        return dispatchRepo.findById(id);
    }

    @Override
    public List<Dispatch> getAllDispatches() {
        return dispatchRepo.findAll();
    }

    @Override
    public Optional<Dispatch> updateDispatch(Long id, Dispatch updatedDispatch) {
        return dispatchRepo.findById(id).map(existingDispatch -> {
            updatedDispatch.setId(existingDispatch.getId());
            saveRelatedEntities(existingDispatch, updatedDispatch);
            return dispatchRepo.save(updatedDispatch);
        });
    }

    @Override
    public boolean deleteDispatch(Long id) {
        if (dispatchRepo.existsById(id)) {
            dispatchRepo.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean manualDispatch(Long id) {
        if (dispatchRepo.existsById(id)) {
            executeDispatch(id);
            return true;
        }
        return false;
    }

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

    // Helper: Create a single DispatchedTest object
    private DispatchedTest createDispatchedTest(Dispatch dispatch, Long formId, Integer personnelId, LocalDateTime dispatchTime) {
        DispatchedTest test = new DispatchedTest();
        test.setDispatch(dispatch);
        test.setFormId(formId);
        test.setPersonnelId(Long.valueOf(personnelId));
        test.setDispatchTime(dispatchTime);
        test.setStatus("PENDING");
        return test;
    }

    // Helper: Increment executed count and save the dispatch
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