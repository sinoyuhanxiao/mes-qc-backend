package com.fps.svmes.controllers;


import com.fps.svmes.dto.requests.DispatchRequest;
import com.fps.svmes.dto.responses.ResponseResult;
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
@RequestMapping("/dispatch")
public class DispatchController {

    @Autowired
    private DispatchService dispatchService;

    @PostMapping
    public ResponseEntity<?> createDispatch(@RequestBody @Valid DispatchRequest request) {
        Dispatch dispatch = dispatchService.createDispatch(request);
        return ResponseEntity.ok(dispatch);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDispatch(@PathVariable Long id, @RequestBody @Valid DispatchRequest request) {
        Dispatch updatedDispatch = dispatchService.updateDispatch(id, request);
        return ResponseEntity.ok(updatedDispatch);
    }

    /**
     * Delete a dispatch by ID.
     * @param id The ID of the dispatch.
     * @return Success message.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDispatch(@PathVariable Long id) {
        dispatchService.deleteDispatch(id);
        return ResponseEntity.ok("Dispatch with ID " + id + " deleted successfully.");
    }

    /**
     * Get a single dispatch by ID.
     * @param id The ID of the dispatch.
     * @return The Dispatch entity.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Dispatch> getDispatch(@PathVariable Long id) {
        Dispatch dispatch = dispatchService.getDispatch(id);
        return ResponseEntity.ok(dispatch);
    }

    /**
     * Get all dispatch records.
     * @return A list of all dispatch entities.
     */
    @GetMapping
    public ResponseEntity<List<Dispatch>> getAllDispatches() {
        List<Dispatch> dispatches = dispatchService.getAllDispatches();
        return ResponseEntity.ok(dispatches);
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



