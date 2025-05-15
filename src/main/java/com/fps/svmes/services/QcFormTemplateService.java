package com.fps.svmes.services;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;

import java.util.List;

public interface QcFormTemplateService {
    List<QcFormTemplateDTO> getAllActiveTemplates();
    QcFormTemplateDTO getTemplateById(Long id);
    QcFormTemplateDTO createTemplate(QcFormTemplateDTO dto);
    QcFormTemplateDTO updateTemplate(Long id, QcFormTemplateDTO dto);
    void deleteTemplate(Long id);
    void extractControlLimits(JsonNode widgetList, ObjectNode controlLimits);
    void createControlLimitSetting(QcFormTemplateDTO template);
    String getApprovalTypeByFormId(Long formTemplateId);
    String resolveLabelFromTemplateByKey(Long templateId, String fieldKey);
}