package com.fps.svmes.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.bson.Document; // Ensure this import is in your class

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
            // Check if the collection exists
            if (!mongoTemplate.collectionExists(collectionName)) {
                return ResponseEntity.status(400)
                        .body("Error: Collection '" + collectionName + "' does not exist.");
            }

            Map<String, Object> document = new HashMap<>(formData);
            document.put("created_at", LocalDateTime.now().toString());
            document.put("created_by", userId);

            // Insert document and retrieve the inserted document
            Document insertedDocument = mongoTemplate.insert(new Document(document), collectionName);

            // Create a response containing the ObjectId
            Map<String, Object> response = new HashMap<>();
            response.put("object_id", insertedDocument.getObjectId("_id").toString());
            response.put("message", "Form data inserted successfully to " + collectionName);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error inserting form data: " + e.getMessage());
        }
    }
}
