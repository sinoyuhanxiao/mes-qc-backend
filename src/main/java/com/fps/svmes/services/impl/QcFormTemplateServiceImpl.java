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
import java.util.List;
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
    public void extractNumberFields(JsonNode widgetList, ObjectNode controlLimits) {
        if (widgetList == null || !widgetList.isArray()) return;

        ObjectMapper mapper = new ObjectMapper();

        for (JsonNode widget : widgetList) {
            String type = widget.get("type").asText();

            if ("grid".equals(type)) {
                for (JsonNode col : widget.get("cols")) {
                    extractNumberFields(col.get("widgetList"), controlLimits);
                }
            } else if ("number".equals(type)) {
                JsonNode options = widget.get("options");
                if (options != null && options.has("name") && options.has("label")) {
                    String name = options.get("name").asText();
                    String label = options.get("label").asText();

                    ObjectNode limit = mapper.createObjectNode();
                    limit.put("upper_control_limit", 100.00); // or derive from options.get("max")
                    limit.put("lower_control_limit", 0.00);   // or derive from options.get("min")
                    limit.put("label", label);

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

            extractNumberFields(widgetList, controlLimits);
            controlLimitDoc.set("control_limits", controlLimits);
            Document mongoDoc = Document.parse(mapper.writeValueAsString(controlLimitDoc));
            mongoService.insertOne("control_limit_setting", mongoDoc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create control limit setting", e);
        }
    }

}
