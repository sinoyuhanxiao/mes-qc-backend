package com.fps.svmes.controllers;

import com.fps.svmes.models.nosql.FormNode;
import com.fps.svmes.services.FormNodeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/form-nodes")
@Tag(name = "Form Management API", description = "API for managing form nodes")
public class FormNodeController {

    @Autowired
    private FormNodeService service;

    // Create or update a node
    @PostMapping
    public FormNode createOrUpdateNode(@RequestBody FormNode node) {
        return service.saveNode(node);
    }

    // Get all top-level nodes
    @GetMapping
    public List<FormNode> getAllNodes() {
        return service.getAllNodes();
    }

    // Get a node by ID (ObjectId or UUID)
    @GetMapping("/{id}")
    public ResponseEntity<FormNode> getNodeById(@PathVariable String id) {
        Optional<FormNode> node = service.getNodeByIdOrUuid(id);
        return node.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete a node by ID (ObjectId or UUID)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNode(@PathVariable String id) {
        boolean deleted = service.deleteNodeByIdOrUuid(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
