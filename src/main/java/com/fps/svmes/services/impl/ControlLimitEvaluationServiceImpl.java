package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.alert.AlertRecordDTO;
import com.fps.svmes.services.AlertRecordService;
import com.fps.svmes.services.ControlLimitEvaluationService;
import com.fps.svmes.services.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ControlLimitEvaluationServiceImpl implements ControlLimitEvaluationService {

    private final RecipeService recipeService;
    private final AlertRecordService alertRecordService;

    @Override
    public void evaluateAndTriggerAlerts(Long templateId, Long userId, Map<String, Object> formData) {
        var setting = recipeService.getByQcFormTemplateId(templateId);
        if (setting == null || setting.getControlLimits() == null) return;

        setting.getControlLimits().forEach((fieldKey, limit) -> {
            Object valueObj = formData.get(fieldKey);
            if (valueObj == null) return;

            try {
                double value = Double.parseDouble(valueObj.toString());
                Double upper = limit.getUpperControlLimit();
                Double lower = limit.getLowerControlLimit();
                boolean isAbove = upper != null && value > upper;
                boolean isBelow = lower != null && value < lower;

                if (isAbove || isBelow) {
                    AlertRecordDTO alert = new AlertRecordDTO();
                    alert.setQcFormTemplateId(templateId);
                    alert.setInspectionValue(BigDecimal.valueOf(value));
                    alert.setUpperControlLimit(upper == null ? null : BigDecimal.valueOf(upper));
                    alert.setLowerControlLimit(lower == null ? null : BigDecimal.valueOf(lower));
                    alert.setRpn(50); // hardcoded to 50 for now
                    alert.setAlertTime(OffsetDateTime.now());
                    alert.setCreatedAt(OffsetDateTime.now());
                    alert.setCreatedBy(userId.intValue());
                    alert.setAlertStatus(0); // defaulted
                    alert.setStatus(1); // not archived

                    // generate alert code according to system time like "AL20250429143152384"
                    String timestampCode = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
                    String alertCode = "AL" + timestampCode;
                    alert.setAlertCode(alertCode);

                    alertRecordService.create(alert);
                }
            } catch (NumberFormatException e) {
                // skip non-numeric fields
            }
        });
    }
}
