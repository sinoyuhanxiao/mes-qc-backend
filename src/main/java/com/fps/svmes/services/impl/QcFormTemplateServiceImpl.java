package com.fps.svmes.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;
import com.fps.svmes.models.sql.qcForm.QcFormTemplate;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcFormTemplateRepository;
import com.fps.svmes.services.MongoService;
import com.fps.svmes.services.QcFormTemplateService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.bson.Document;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QcFormTemplateServiceImpl implements QcFormTemplateService {

    @Autowired
    private QcFormTemplateRepository repository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private MongoService mongoService; // Assuming you already have this bean

    @Override
    public List<QcFormTemplateDTO> getAllActiveTemplates() {
        return repository.findAllByStatus(1).stream()
                .map(template -> modelMapper.map(template, QcFormTemplateDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public QcFormTemplateDTO getTemplateById(Long id) {
        QcFormTemplate template = repository.findById(id).orElseThrow(() -> new RuntimeException("Template not found"));
        return modelMapper.map(template, QcFormTemplateDTO.class);
    }

    @Override
    public QcFormTemplateDTO createTemplate(QcFormTemplateDTO dto) {
        QcFormTemplate template = modelMapper.map(dto, QcFormTemplate.class);
        template.setCreatedAt(OffsetDateTime.now());
        template.setStatus(1);
        template.setApprovalType(dto.getApprovalType());
        return modelMapper.map(repository.save(template), QcFormTemplateDTO.class);
    }

    @Override
    public QcFormTemplateDTO updateTemplate(Long id, QcFormTemplateDTO dto) {
        QcFormTemplate template = repository.findById(id).orElseThrow(() -> new RuntimeException("Template not found"));

        if (dto.getName() != null) {
            template.setName(dto.getName());
        }
        if (dto.getFormTemplateJson() != null) {
            template.setFormTemplateJson(dto.getFormTemplateJson());
        } else {
            template.setFormTemplateJson(template.getFormTemplateJson());
        }
        if (dto.getApprovalType() != null) {
            template.setApprovalType(dto.getApprovalType());  // 更新审批类型（可选）
        }
        template.setUpdatedAt(OffsetDateTime.now());
        template.setUpdatedBy(dto.getUpdatedBy());

        return modelMapper.map(repository.save(template), QcFormTemplateDTO.class);
    }

    @Override
    public void deleteTemplate(Long id) {
        QcFormTemplate template = repository.findById(id).orElseThrow(() -> new RuntimeException("Template not found"));
        template.setStatus(0);
        repository.save(template);
    }

    @Override
    public void extractControlLimits(JsonNode widgetList, ObjectNode controlLimits) {
        if (widgetList == null || !widgetList.isArray()) return;

        ObjectMapper mapper = new ObjectMapper();

        for (JsonNode widget : widgetList) {
            String type = widget.get("type").asText();

            // 递归解析 grid/col 中的 widgetList
            if ("grid".equals(type)) {
                for (JsonNode col : widget.get("cols")) {
                    extractControlLimits(col.get("widgetList"), controlLimits);
                }
            }

            // 处理数值字段（number）
            else if ("number".equals(type)) {
                JsonNode options = widget.get("options");
                if (options != null && options.has("name") && options.has("label")) {
                    String name = options.get("name").asText();
                    String label = options.get("label").asText();

                    ObjectNode limit = mapper.createObjectNode();
                    limit.put("upper_control_limit", 100.00); // 可选从 options.get("max")
                    limit.put("lower_control_limit", 0.00);   // 可选从 options.get("min")
                    limit.put("label", label);

                    controlLimits.set(name, limit);
                }
            }

            // 处理选项字段（select、radio、checkbox）
            else if (type.equals("select") || type.equals("radio") || type.equals("checkbox")) {
                JsonNode options = widget.get("options");
                if (options != null && options.has("name") && options.has("label") && options.has("optionItems")) {
                    String name = options.get("name").asText();
                    String label = options.get("label").asText();
                    JsonNode optionItems = options.get("optionItems");

                    ObjectNode limit = mapper.createObjectNode();
                    limit.put("label", label);

                    List<String> validKeys = new ArrayList<>();
                    for (JsonNode item : optionItems) {
                        validKeys.add(item.get("value").asText());
                    }
                    limit.putPOJO("valid_keys", validKeys);

                    List<Map<String, String>> optionList = new ArrayList<>();
                    for (JsonNode item : optionItems) {
                        Map<String, String> optionMap = new HashMap<>();
                        optionMap.put("label", item.path("label").asText());
                        optionMap.put("value", item.path("value").asText());
                        optionList.add(optionMap);
                    }
                    limit.putPOJO("optionItems", optionList);

                    controlLimits.set(name, limit);
                }
            }
        }
    }


    @Override
    public void createControlLimitSetting(QcFormTemplateDTO template) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(template.getFormTemplateJson());
            JsonNode widgetList = root.get("widgetList");

            ObjectNode controlLimitDoc = mapper.createObjectNode();
            controlLimitDoc.put("qc_form_template_id", template.getId());
            ObjectNode controlLimits = mapper.createObjectNode();

            extractControlLimits(widgetList, controlLimits);
            controlLimitDoc.set("control_limits", controlLimits);
            Document mongoDoc = Document.parse(mapper.writeValueAsString(controlLimitDoc));
            mongoService.insertOne("control_limit_setting", mongoDoc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create control limit setting", e);
        }
    }

    @Override
    public String resolveLabelFromTemplateByKey(Long templateId, String fieldKey) {
        QcFormTemplate template = repository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(template.getFormTemplateJson());
            JsonNode widgetList = root.get("widgetList");
            return findLabelInWidgetList(widgetList, fieldKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve label from form template", e);
        }
    }

    private String findLabelInWidgetList(JsonNode widgetList, String fieldKey) {
        if (widgetList == null || !widgetList.isArray()) return null;

        for (JsonNode widget : widgetList) {
            String type = widget.get("type").asText();

            if ("grid".equals(type)) {
                for (JsonNode col : widget.get("cols")) {
                    String label = findLabelInWidgetList(col.get("widgetList"), fieldKey);
                    if (label != null) return label;
                }
            } else if ("number".equals(type)) {
                JsonNode options = widget.get("options");
                if (options != null && options.has("name") && options.has("label")) {
                    if (fieldKey.equals(options.get("name").asText())) {
                        return options.get("label").asText();
                    }
                }
            }
        }

        return null; // not found
    }


}
