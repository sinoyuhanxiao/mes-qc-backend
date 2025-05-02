package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.alert.AlertRecordDTO;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcFormTemplateRepository;
import com.fps.svmes.services.AlertRecordService;
import com.fps.svmes.services.ControlLimitEvaluationService;
import com.fps.svmes.services.QcFormTemplateService;
import com.fps.svmes.services.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ControlLimitEvaluationServiceImpl implements ControlLimitEvaluationService {

    private final RecipeService recipeService;
    private final AlertRecordService alertRecordService;
    private final QcFormTemplateService qcFormTemplateService;
    private final QcFormTemplateRepository qcFormTemplateRepository;

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

                    // Safely extract arrays from formData and convert to List<Long>
                    Object productObj = formData.get("related_product_ids");
                    if (productObj instanceof List<?> productList && !productList.isEmpty()) {
                        alert.setProductIds(productList.stream().map(Object::toString).map(Long::valueOf).toList());
                    }

                    Object batchObj = formData.get("related_batch_ids");
                    if (batchObj instanceof List<?> batchList && !batchList.isEmpty()) {
                        alert.setBatchIds(batchList.stream().map(Object::toString).map(Long::valueOf).toList());
                    }

                    Object inspectorObj = formData.get("related_inspector_ids");
                    if (inspectorObj instanceof List<?> inspectorList && !inspectorList.isEmpty()) {
                        alert.setInspectorIds(inspectorList.stream().map(Object::toString).map(Long::valueOf).toList());
                    }

                    Object reviewerObj = formData.get("related_reviewer_ids");
                    if (reviewerObj instanceof List<?> reviewerList && !reviewerList.isEmpty()) {
                        alert.setReviewerIds(reviewerList.stream().map(Object::toString).map(Long::valueOf).toList());
                    }

                    alert.setQcFormTemplateId(templateId);
                    alert.setInspectionItemKey(fieldKey);
                    alert.setInspectionItemLabel(qcFormTemplateService.resolveLabelFromTemplateByKey(templateId, fieldKey));
                    alert.setInspectionValue(BigDecimal.valueOf(value));
                    alert.setUpperControlLimit(upper == null ? null : BigDecimal.valueOf(upper));
                    alert.setLowerControlLimit(lower == null ? null : BigDecimal.valueOf(lower));
                    alert.setRpn(50); // hardcoded to 50 for now
                    alert.setAlertTime(OffsetDateTime.now());
                    alert.setCreatedAt(OffsetDateTime.now());
                    alert.setCreatedBy(userId.intValue());
                    alert.setAlertStatus(1); // default to 处理中
                    alert.setStatus(1); // default to active

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
