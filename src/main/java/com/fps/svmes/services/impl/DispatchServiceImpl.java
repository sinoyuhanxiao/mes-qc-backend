package com.fps.svmes.services.impl;

import com.fps.svmes.models.sql.Dispatch;
import com.fps.svmes.models.sql.DispatchedTest;
import com.fps.svmes.models.sql.ScheduleType;
import com.fps.svmes.repositories.jpaRepo.DispatchRepository;
import com.fps.svmes.repositories.jpaRepo.DispatchedTestRepository;
import com.fps.svmes.services.DispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Implementation of the DispatchService interface.
 */
@Service
public class DispatchServiceImpl implements DispatchService {

    @Autowired
    private DispatchRepository dispatchRepo;

    @Autowired
    private DispatchedTestRepository testRepo;

    // FOR WECOM LATER
//    @Value("${wechat.api.url}")
//    private String wechatApiUrl;
//
//    @Value("${wechat.access.token}")
//    private String wechatAccessToken;
//
//    @Value("${wechat.template.id}")
//    private String wechatTemplateId;

    @Override
    public void executeDispatch(Long dispatchId) {
        Dispatch dispatch = dispatchRepo.findById(dispatchId).orElseThrow();

        List<Long> personnelList = dispatch.getTargetPersonnel();
        List<Long> formIds = dispatch.getFormIds();

        for (Long formId : formIds) {
            for (Long personnelId : personnelList) {
                String formUrl = generateFormUrl(formId, personnelId);
                simulateNotification(personnelId, formUrl);

                DispatchedTest test = new DispatchedTest();
                test.setDispatch(dispatch);
                test.setPersonnelId(personnelId);
                test.setFormId(formId);
                test.setDispatchTime(LocalDateTime.now());
                test.setStatus("PENDING");
                testRepo.save(test);
            }
        }

        // Increment the executed count
        dispatch.setExecutedCount(dispatch.getExecutedCount() + 1);
        dispatchRepo.save(dispatch);
    }

    @Override
    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    public void scheduleDispatches() {
        List<Dispatch> activeDispatches = dispatchRepo.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Dispatch dispatch : activeDispatches) {
            if (dispatch.isActive() && shouldDispatch(dispatch, now)) {
                executeDispatch(dispatch.getId());
            }
        }
    }

    public boolean shouldDispatch(Dispatch dispatch, LocalDateTime now) {
        if (dispatch.getScheduleType() == ScheduleType.SPECIFIC_DAYS) {
            String currentDay = now.getDayOfWeek().name();
            String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));

            return dispatch.getSpecificDays().contains(currentDay) &&
                    currentTime.equals(dispatch.getTimeOfDay());
        } else if (dispatch.getScheduleType() == ScheduleType.INTERVAL) {
            if (dispatch.getRepeatCount() != null &&
                    dispatch.getExecutedCount() >= dispatch.getRepeatCount()) {
                return false; // Dispatch has already been executed the maximum number of times
            }

            LocalDateTime nextDispatchTime = dispatch.getStartTime().plusMinutes(
                    dispatch.getIntervalMinutes() * dispatch.getExecutedCount());
            return !now.isBefore(nextDispatchTime);
        }
        return false;
    }

    /**
     * Generates a unique URL for a form assigned to a personnel.
     *
     * @param formId the ID of the form
     * @param personnelId the ID of the personnel
     * @return the generated URL
     */
    private String generateFormUrl(Long formId, Long personnelId) {
        return "https://your-system.com/forms/" + formId + "?user=" + personnelId;
    }

    /**
     * Simulates sending a notification by printing to the console.
     *
     * @param personnelId the ID of the personnel
     * @param formUrl the URL of the form
     */
    private void simulateNotification(Long personnelId, String formUrl) {
        System.out.println("Simulating notification to Personnel ID: " + personnelId + " with Form URL: " + formUrl);
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