package com.fps.svmes.controllers;


import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
import com.fps.svmes.dto.dtos.dispatch.DispatchedTaskDTO;
import com.fps.svmes.dto.requests.DispatchRequest;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.services.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing dispatch configurations.
 */
@RestController
@RequestMapping("/dispatch")
@Tag(name = "Dispatch API", description = "API for QC Test Dispatch")
public class DispatchController {

    @Autowired
    private DispatchService dispatchService;

    @Operation(summary = "Create a new dispatch", description = "Creates a dispatch in the QC System")
    @PostMapping
    public ResponseResult<DispatchDTO> createDispatch(@RequestBody @Valid DispatchRequest request) {
        try {
            DispatchDTO dispatchDTO = dispatchService.createDispatch(request);
            return ResponseResult.success(dispatchDTO);
        } catch (IllegalArgumentException e) {
            return ResponseResult.failBadRequest("Failed to create dispatch: " + e.getMessage(), e);
        } catch (Exception e) {
            return ResponseResult.fail("Unexpected error occurred while creating dispatch", e);
        }
    }

    @Operation(summary = "Get a single dispatch by ID", description = "Retrieves a dispatch by its ID")
    @GetMapping("/{id}")
    public ResponseResult<DispatchDTO> getDispatch(@PathVariable Long id) {
        try {
            DispatchDTO dispatchDTO = dispatchService.getDispatch(id);
            return ResponseResult.success(dispatchDTO);
        } catch (IllegalArgumentException e) {
            return ResponseResult.failNotFound("Dispatch not found with ID: " + id, e);
        } catch (Exception e) {
            return ResponseResult.fail("Unexpected error occurred while retrieving dispatch", e);
        }
    }


    @Operation(summary = "Get all dispatches", description = "Retrieves a list of all dispatches")
    @GetMapping
    public ResponseResult<List<DispatchDTO>> getAllDispatches() {
        try {
            List<DispatchDTO> dispatches = dispatchService.getAllDispatches();
            return dispatches.isEmpty()
                    ? ResponseResult.noContent(dispatches)
                    : ResponseResult.success(dispatches);
        } catch (Exception e) {
            return ResponseResult.fail("Unexpected error occurred while retrieving all dispatches", e);
        }
    }

    @Operation(summary = "Get all dispatched tests", description = "Retrieves a list of all dispatched tests")
    @GetMapping("/dispatched-tasks")
    public ResponseResult<List<DispatchedTaskDTO>> getAllDispatchedTask() {
        try {
            List<DispatchedTaskDTO> tasks = dispatchService.getAllDispatchedTasks();
            return tasks.isEmpty()
                    ? ResponseResult.noContent(tasks)
                    : ResponseResult.success(tasks);
        } catch (Exception e) {
            return ResponseResult.fail("Unexpected error occurred while retrieving all dispatched tasks", e);
        }
    }

    @Operation(summary = "Update an existing dispatch", description = "Updates a dispatch given an ID")
    @PutMapping("/{id}")
    public ResponseResult<DispatchDTO> updateDispatch(@PathVariable Long id, @RequestBody @Valid DispatchRequest request) {
        try {
            DispatchDTO dispatchDTO = dispatchService.updateDispatch(id, request);
            return ResponseResult.success(dispatchDTO);
        } catch (IllegalArgumentException e) {
            return ResponseResult.failBadRequest("Failed to update dispatch: " + e.getMessage(), e);
        } catch (Exception e) {
            return ResponseResult.fail("Unexpected error occurred while updating dispatch", e);
        }
    }

    @Operation(summary = "Delete a dispatch", description = "Deletes a dispatch given its ID")
    @DeleteMapping("/{id}")
    public ResponseResult<String> deleteDispatch(@PathVariable Long id) {
        try {
            dispatchService.deleteDispatch(id);
            return ResponseResult.success("Dispatch with ID " + id + " deleted successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseResult.failNotFound("Failed to delete dispatch: " + e.getMessage(), e);
        } catch (EntityNotFoundException e) {
            return ResponseResult.failNotFound(e.getMessage(), e);
        }
        catch (Exception e) {
            return ResponseResult.fail("Unexpected error occurred while deleting dispatch", e);
        }
    }

    @Operation(summary = "Manually trigger a dispatch", description = "Manually triggers a dispatch execution")
    @PostMapping("/manual_trigger/{id}")
    public ResponseResult<String> manualDispatch(@PathVariable Long id) {
        try {
            boolean success = dispatchService.manualDispatch(id);
            if (success) {
                return ResponseResult.success("Dispatch executed successfully for ID: " + id);
            } else {
                return ResponseResult.failNotFound("No dispatch found with ID: " + id, null);
            }
        } catch (Exception e) {
            return ResponseResult.fail("Unexpected error occurred while manually triggering dispatch", e);
        }
    }



}



