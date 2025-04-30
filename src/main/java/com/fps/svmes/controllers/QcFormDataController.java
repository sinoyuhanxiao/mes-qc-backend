package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;
import com.fps.svmes.services.ControlLimitEvaluationService;
import com.fps.svmes.services.QcFormTemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.bson.Document;
import com.fps.svmes.services.RecipeService;
import com.fps.svmes.services.AlertRecordService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/qc-form-data")
@RequiredArgsConstructor
@Tag(name = "QC Form Data API", description = "API for QC Form Data")
public class QcFormDataController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private QcFormTemplateService qcFormTemplateService;

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private AlertRecordService alertRecordService;

    @Autowired
    private ControlLimitEvaluationService controlLimitEvaluationService;

    @PostMapping("/insert-form/{userId}/{collectionName}")
    public ResponseEntity<?> insertFormData(
            @PathVariable String collectionName,
            @PathVariable Long userId,
            @RequestBody Map<String, Object> formData) {
        try {
            // Extract formTemplate ID from collection name (e.g., form_template_<ID>_<YEARMONTH>)
            String[] parts = collectionName.split("_");
            if (parts.length < 3) {
                return ResponseEntity.status(400).body("Invalid collection name format: " + collectionName);
            }

            Long formTemplateId;
            try {
                formTemplateId = Long.parseLong(parts[2]);
            } catch (NumberFormatException e) {
                return ResponseEntity.status(400).body("Invalid form template ID in collection name: " + collectionName);
            }

            // Check if the collection exists
            if (!mongoTemplate.collectionExists(collectionName)) {
                // Check if the template exists using QcFormTemplateService
                QcFormTemplateDTO template = qcFormTemplateService.getTemplateById(formTemplateId);
                if (template == null) {
                    return ResponseEntity.status(400).body("Error: Template ID " + formTemplateId + " does not exist. Cannot create collection.");
                }

                // Create the collection dynamically
                mongoTemplate.createCollection(collectionName);
                log.info("Created new collection: {}", collectionName);
            }

            // Insert the form data
            Map<String, Object> document = new HashMap<>(formData);
            document.put("created_at", LocalDateTime.now().toString());
            document.put("created_by", userId);

            Document insertedDocument = mongoTemplate.insert(new Document(document), collectionName);

            // Evaluate control limits and trigger alerts if needed
            controlLimitEvaluationService.evaluateAndTriggerAlerts(formTemplateId, userId, formData);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("object_id", insertedDocument.getObjectId("_id").toString());
            response.put("message", "Form data inserted successfully to " + collectionName);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error inserting form data", e);
            return ResponseEntity.status(500).body("Error inserting form data: " + e.getMessage());
        }
    }
}
