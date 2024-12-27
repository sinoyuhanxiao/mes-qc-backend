package com.fps.svmes.controllers;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestFormController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostMapping("/insert-form/{collectionName}")
    public ResponseEntity<?> insertFormData(
            @PathVariable String collectionName,
            @RequestBody Map<String, Object> formData) {
        try {
            Map<String, Object> document = new HashMap<>(formData);
            document.put("inserted_at", LocalDateTime.now().toString());

            // Insert into the specified collection
            mongoTemplate.insert(document, collectionName);

            return ResponseEntity.ok("Form data inserted into collection: " + collectionName);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error inserting form data: " + e.getMessage());
        }
    }
}
