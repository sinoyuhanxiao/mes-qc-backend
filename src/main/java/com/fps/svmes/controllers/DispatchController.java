package com.fps.svmes.controllers;


import com.fps.svmes.models.sql.task_schedule.Dispatch;
import com.fps.svmes.services.DispatchService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing dispatch configurations.
 */
@RestController
@RequestMapping("/api/dispatches")
public class DispatchController {

    @Autowired
    private DispatchService dispatchService;

    /**
     * Creates a new dispatch configuration.
     */
    @PostMapping
    public ResponseEntity<Dispatch> createDispatch(@RequestBody @Valid Dispatch dispatch) {
        Dispatch createdDispatch = dispatchService.createDispatch(dispatch);
        return ResponseEntity.status(201).body(createdDispatch);
    }

    /**
     * Retrieves all dispatch configurations.
     */
    @GetMapping
    public ResponseEntity<List<Dispatch>> getAllDispatches() {
        return ResponseEntity.ok(dispatchService.getAllDispatches());
    }

    /**
     * Retrieves a specific dispatch configuration by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Dispatch> getDispatchById(@PathVariable Long id) {
        return dispatchService.getDispatchById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates an existing dispatch configuration.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Dispatch> updateDispatch(
            @PathVariable Long id,
            @RequestBody @Valid Dispatch dispatch) {
        return dispatchService.updateDispatch(id, dispatch)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deletes a dispatch configuration by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDispatch(@PathVariable Long id) {
        if (dispatchService.deleteDispatch(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Manually triggers the dispatch process for a specific configuration.
     */
    @PostMapping("/dispatch/{id}")
    public ResponseEntity<String> manualDispatch(@PathVariable Long id) {
        boolean success = dispatchService.manualDispatch(id);
        return success
                ? ResponseEntity.ok("Dispatch executed successfully for ID: " + id)
                : ResponseEntity.notFound().build();
    }
}



