package com.fps.svmes.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/insert-form/{userId}/{collectionName}")
    public ResponseEntity<?> insertFormData(
            @PathVariable String collectionName,
            @PathVariable Long userId,
            @RequestBody Map<String, Object> formData) {
        try {
            Map<String, Object> document = new HashMap<>(formData);
            document.put("created_at", LocalDateTime.now().toString());
            document.put("created_by", userId);

            // Insert into the specified collection
            mongoTemplate.insert(document, collectionName);

            return ResponseEntity.ok("Form data inserted into collection: " + collectionName);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error inserting form data: " + e.getMessage());
        }
    }
}
