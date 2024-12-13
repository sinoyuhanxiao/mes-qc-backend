package com.fps.svmes.services.impl;

import com.fps.svmes.models.sql.Dispatch;
import com.fps.svmes.models.sql.DispatchedTest;
import com.fps.svmes.repositories.jpaRepo.DispatchRepository;
import com.fps.svmes.repositories.jpaRepo.DispatchedTestRepository;
import com.fps.svmes.services.DispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
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

    @Value("${wechat.api.url}")
    private String wechatApiUrl;

    @Value("${wechat.access.token}")
    private String wechatAccessToken;

    @Value("${wechat.template.id}")
    private String wechatTemplateId;

    @Override
    public void executeDispatch(Long dispatchId) {
        Dispatch dispatch = dispatchRepo.findById(dispatchId).orElseThrow();

        List<Long> personnelList = dispatch.getTargetPersonnel();
        List<Long> formIds = dispatch.getFormIds();

        for (Long formId : formIds) {
            for (Long personnelId : personnelList) {
                String formUrl = generateFormUrl(formId, personnelId);
                notifyPersonnel(personnelId, formUrl);

                DispatchedTest test = new DispatchedTest();
                test.setDispatch(dispatch);
                test.setPersonnelId(personnelId);
                test.setFormId(formId);
                test.setDispatchTime(LocalDateTime.now());
                test.setStatus("PENDING");
                testRepo.save(test);
            }
        }
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
     * Sends a WeChat notification to the personnel with the form URL.
     *
     * @param personnelId the ID of the personnel
     * @param formUrl the URL of the form
     */
    private void notifyPersonnel(Long personnelId, String formUrl) {
        String url = wechatApiUrl + "/cgi-bin/message/template/send?access_token=" + wechatAccessToken;
        RestTemplate restTemplate = new RestTemplate();

        var payload = new java.util.HashMap<String, Object>();
        payload.put("touser", "wechat-id-for-personnel-" + personnelId); // Replace with actual retrieval of WeChat ID

        payload.put("template_id", wechatTemplateId);
        payload.put("url", formUrl);
        payload.put("data", java.util.Map.of(
                "first", java.util.Map.of("value", "You have a new QC test assignment."),
                "keyword1", java.util.Map.of("value", "Form Link"),
                "keyword2", java.util.Map.of("value", "High Priority"),
                "remark", java.util.Map.of("value", "Please complete it as soon as possible.")
        ));

        try {
            restTemplate.postForObject(url, payload, String.class);
            System.out.println("WeChat notification sent for Personnel ID: " + personnelId);
        } catch (Exception e) {
            System.err.println("Failed to send WeChat notification: " + e.getMessage());
        }
    }
}