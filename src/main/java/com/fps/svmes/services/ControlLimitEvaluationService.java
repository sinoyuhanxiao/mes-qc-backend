package com.fps.svmes.services;

import java.util.Map;

public interface ControlLimitEvaluationService {
    void evaluateAndTriggerAlerts(Long templateId, Long userId, Map<String, Object> formData);
}