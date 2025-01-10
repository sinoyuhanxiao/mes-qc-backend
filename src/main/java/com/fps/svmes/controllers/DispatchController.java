package com.fps.svmes.controllers;


import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
import com.fps.svmes.dto.dtos.dispatch.DispatchedTaskDTO;
import com.fps.svmes.dto.requests.DispatchRequest;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.models.sql.taskSchedule.Dispatch;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchRepository;
import com.fps.svmes.services.DispatchService;
import com.fps.svmes.services.TaskScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
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

    private static final Logger logger = LoggerFactory.getLogger(DispatchController.class);


    @Operation(summary = "Create a new dispatch", description = "Creates a dispatch in the QC System")
    @PostMapping
    public ResponseResult<DispatchDTO> createDispatch(@RequestBody DispatchRequest request) {
        try {
            DispatchDTO dispatchDTO = dispatchService.createDispatch(request);
            return ResponseResult.success(dispatchDTO);
        } catch (Exception e) {
            logger.error("Error creating dispatch", e);
            return ResponseResult.fail("Failed to create dispatch", e);
        }
    }

    @Operation(summary = "Create a manual dispatch", description = "Create a manual dispatch that dispatches right away")
    @PostMapping("/manual")
    public ResponseResult<DispatchDTO> createManualDispatch(@RequestBody DispatchRequest request) {
        try {
            DispatchDTO dispatchDTO = dispatchService.createManualDispatch(request);
            return ResponseResult.success(dispatchDTO);
        } catch (Exception e) {
            logger.error("Error creating manual dispatch", e);
            return ResponseResult.fail("Failed to create a manual dispatch", e);
        }
    }

    @Operation(summary = "Get a single dispatch by ID", description = "Retrieves a dispatch by its ID")
    @GetMapping("/{id}")
    public ResponseResult<DispatchDTO> getDispatch(@PathVariable Long id) {
        try {
            DispatchDTO dispatchDTO = dispatchService.getDispatch(id);
            return ResponseResult.success(dispatchDTO);
        } catch (Exception e) {
            logger.error("Error retrieving dispatch for ID: {}", id);
            return ResponseResult.fail("Failed to retrieve dispatch", e);
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
            logger.error("Error retrieving all dispatches");
            return ResponseResult.fail("Failed to retrieve all dispatches", e);
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
            logger.error("Error retrieving all dispatched tasks");
            return ResponseResult.fail("Failed to retrieve all dispatched tasks", e);
        }
    }

    @Operation(summary = "Update an existing dispatch", description = "Updates a dispatch given an ID")
    @PutMapping("/{id}")
    public ResponseResult<DispatchDTO> updateDispatch(@PathVariable Long id, @RequestBody @Valid DispatchRequest request) {
        try {
            DispatchDTO dispatchDTO = dispatchService.updateDispatch(id, request);
            return ResponseResult.success(dispatchDTO);
        } catch (Exception e) {
            logger.error("Error updating dispatch with ID: {}",  id);
            return ResponseResult.fail("Failed to update a dispatch", e);
        }
    }

    @Operation(summary = "Delete a dispatch", description = "Deletes a dispatch given its ID")
    @DeleteMapping("/{id}")
    public ResponseResult<String> deleteDispatch(@PathVariable Long id) {
        try {
            dispatchService.deleteDispatch(id);
            return ResponseResult.success("Dispatch with ID " + id + " deleted successfully.");
        } catch (Exception e) {
            logger.error("Error deleting a dispatch with ID: {}", id );
            return ResponseResult.fail("Failed to delete a dispatch", e);
        }
    }

    /**
     * Endpoint to schedule a dispatch task.
     *
     * @param id the ID of the dispatch to schedule.
     * @return success or failure response.
     */
    @Operation(summary = "Schedule dispatch task for the specified dispatch", description = "Schedule dispatch task for the specified dispatch")
    @PostMapping("/schedule/{id}")
    public ResponseResult<String> scheduleTask(@PathVariable Long id) {
        try {

            Dispatch dispatch = dispatchRepository.findByIdAndStatus(id, 1);
            dispatchService.scheduleDispatch(dispatch.getId(), () -> dispatchService.executeDispatch(dispatch.getId()));
            return ResponseResult.success("Task scheduled successfully for Dispatch ID: " + id);
        } catch (Exception e) {
            logger.error("Error scheduling a dispatch with ID: {}", id);
            return ResponseResult.fail("Failed to schedule a dispatch", e);
        }
    }

    /**
     * Endpoint to cancel a scheduled task.
     *
     * @param id the ID of the dispatch to cancel.
     * @return success or failure response.
     */
    @Operation(summary = "Cancel dispatch task for specified dispatch", description = "Schedule dispatch task for the specified dispatch")
    @DeleteMapping("/cancel/{id}")
    public ResponseResult<String> cancelTask(@PathVariable Long id) {

        try {
            dispatchService.cancelDispatchTask(id);
            return ResponseResult.success("Task cancelled successfully for Dispatch ID: " + id);
        }
        catch (Exception e) {
            logger.error("Error canceling a dispatch with ID: {}", id);
            return ResponseResult.fail("Failed to cancel a dispatch", e);
        }
    }

    /**
     * Endpoint to check if a task is scheduled.
     *
     * @param id the ID of the dispatch to check.
     * @return true if the task is scheduled, false otherwise.
     */
    @GetMapping("/is-scheduled/{id}")
    public ResponseResult<Boolean> isTaskScheduled(@PathVariable Long id) {

        try {
            boolean isScheduled = taskScheduleService.isScheduled(id);
            return ResponseResult.success(isScheduled);
        } catch (Exception e) {
            logger.error("Error checking is scheduled for a dispatch with ID: {}", id);
            return ResponseResult.fail("Fail to check is scheduled for a dispatch with ID", e);
        }
    }


    /**
     * Endpoint to get the next execution time of a scheduled task.
     *
     * @param id the ID of the dispatch to check.
     * @return the next execution time or null if not scheduled.
     */
    @GetMapping("/next-execution-time/{id}")
    public ResponseResult<OffsetDateTime> getNextExecutionTime(@PathVariable Long id) {
        try {
            OffsetDateTime nextExecutionTime = taskScheduleService.getNextExecutionTime(id);
            return ResponseResult.success(nextExecutionTime);
        } catch (Exception e) {
            logger.error("Error getting next execution time for dispatch ID: {}", id);
            return ResponseResult.fail("Fail to get next execution time for dispatch with ID", e);
        }
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
            Map<Long, OffsetDateTime> scheduledTasks = taskScheduleService.getAllScheduledTasks();

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
            logger.error("Error retrieving all scheduled tasks details");
            return ResponseResult.fail("Failed to retrieve all scheduled tasks", e);
        }
    }

}



