package com.fps.svmes.controllers;


import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
import com.fps.svmes.dto.dtos.dispatch.DispatchedTaskDTO;
import com.fps.svmes.dto.requests.DispatchRequest;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.models.sql.task_schedule.Dispatch;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchRepository;
import com.fps.svmes.services.DispatchService;
import com.fps.svmes.services.TaskScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing dispatch configurations.
 */
@RestController
@RequestMapping("/dispatch")
@Tag(name = "Dispatch API", description = "API for QC Test Dispatch")
public class DispatchController {

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private TaskScheduleService taskScheduleService;

    @Autowired
    private DispatchRepository dispatchRepository;

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

    /**
     * Endpoint to schedule a dispatch task.
     *
     * @param dispatchId the ID of the dispatch to schedule.
     * @return success or failure response.
     */
    @Operation(summary = "Schedule dispatch task for the specified dispatch", description = "Schedule dispatch task for the specified dispatch")
    @PostMapping("/schedule/{dispatchId}")
    public ResponseEntity<String> scheduleTask(@PathVariable Long dispatchId) {
        Dispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new IllegalArgumentException("Dispatch not found"));
        dispatchService.scheduleDispatchTask(dispatch.getId(), () -> dispatchService.executeDispatch(dispatch.getId()));
        return ResponseEntity.ok("Task scheduled successfully for Dispatch ID: " + dispatchId);
    }

    /**
     * Endpoint to cancel a scheduled task.
     *
     * @param dispatchId the ID of the dispatch to cancel.
     * @return success or failure response.
     */
    @Operation(summary = "Cancel dispatch task for specified dispatch", description = "Schedule dispatch task for the specified dispatch")
    @DeleteMapping("/cancel/{dispatchId}")
    public ResponseResult<String> cancelTask(@PathVariable Long dispatchId) {

        try {
            dispatchService.cancelDispatchTask(dispatchId);
            return ResponseResult.success("Task cancelled successfully for Dispatch ID: " + dispatchId);
        }
        catch (Exception e) {
            return ResponseResult.fail("Unexpected error occurred while canceling dispatching task", e);
        }
    }

    /**
     * Endpoint to check if a task is scheduled.
     *
     * @param dispatchId the ID of the dispatch to check.
     * @return true if the task is scheduled, false otherwise.
     */
    @GetMapping("/is-scheduled/{dispatchId}")
    public ResponseEntity<Boolean> isTaskScheduled(@PathVariable Long dispatchId) {
        boolean isScheduled = taskScheduleService.isScheduled(dispatchId);
        return ResponseEntity.ok(isScheduled);
    }


    /**
     * Endpoint to get the next execution time of a scheduled task.
     *
     * @param dispatchId the ID of the dispatch to check.
     * @return the next execution time or null if not scheduled.
     */
    @GetMapping("/next-execution-time/{dispatchId}")
    public ResponseEntity<String> getNextExecutionTime(@PathVariable Long dispatchId) {
        Timestamp nextExecutionTime = taskScheduleService.getNextExecutionTime(dispatchId);
        return ResponseEntity.ok("Next execution time for this dispatch is : " + nextExecutionTime);
    }

    /**
     * Endpoint to get all scheduled tasks with their next execution times.
     *
     * @return A response result containing the list of scheduled tasks and their next execution times.
     */
    @Operation(summary = "Get all scheduled tasks", description = "Retrieves all scheduled tasks and their next execution times.")
    @GetMapping("/scheduled-tasks")
    public ResponseResult<List<Map<String, Object>>> getAllScheduledTasks() {
        try {
            Map<Long, Timestamp> scheduledTasks = taskScheduleService.getAllScheduledTasks();

            // Convert Map to List of Maps for JSON-friendly representation
            List<Map<String, Object>> response = scheduledTasks.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> taskInfo = new HashMap<>();
                        taskInfo.put("dispatchId", entry.getKey());
                        taskInfo.put("nextExecutionTime", entry.getValue());
                        return taskInfo;
                    })
                    .toList();

            return response.isEmpty()
                    ? ResponseResult.noContent(response)
                    : ResponseResult.success(response);
        } catch (Exception e) {
            return ResponseResult.fail("Unexpected error occurred while retrieving scheduled tasks", e);
        }
    }

    /**
     * Endpoint to log task details for a given dispatch ID.
     *
     * @param dispatchId the ID of the dispatch to log details for.
     * @return success message.
     */
    @GetMapping("/log-details/{dispatchId}")
    public ResponseEntity<String> logTaskDetails(@PathVariable Long dispatchId) {
        taskScheduleService.logScheduledTaskDetails(dispatchId);
        return ResponseEntity.ok("Logged task details for Dispatch ID: " + dispatchId);
    }


//    @Operation(summary = "Manually trigger a dispatch", description = "Manually triggers a dispatch execution")
//    @PostMapping("/manual_trigger/{id}")
//    public ResponseResult<String> manualDispatch(@PathVariable Long id) {
//        try {
//            boolean success = dispatchService.manualDispatch(id);
//            if (success) {
//                return ResponseResult.success("Dispatch executed successfully for ID: " + id);
//            } else {
//                return ResponseResult.failNotFound("No dispatch found with ID: " + id, null);
//            }
//        } catch (Exception e) {
//            return ResponseResult.fail("Unexpected error occurred while manually triggering dispatch", e);
//        }
//    }



}



