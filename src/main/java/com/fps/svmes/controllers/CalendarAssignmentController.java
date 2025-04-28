package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.formAccessCalendar.CalendarAssignmentDTO;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.services.CalendarAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/calendar-assignment")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Calendar Assignment API", description = "API for managing calendar assignments")
public class CalendarAssignmentController {

    @Autowired
    private CalendarAssignmentService service;

    private static final Logger logger = LoggerFactory.getLogger(CalendarAssignmentController.class);

    @PostMapping
    @Operation(summary = "Create a calendar assignment", description = "Create a new calendar assignment with associated form nodes.")
    public ResponseResult<CalendarAssignmentDTO> create(@RequestBody CalendarAssignmentDTO dto) {
        try {
            CalendarAssignmentDTO created = service.createAssignment(dto);
            logger.info("Created new Calendar Assignment with ID: {}", created.getId());
            return ResponseResult.success(created);
        } catch (Exception e) {
            logger.error("Error creating calendar assignment", e);
            return ResponseResult.fail("Failed to create calendar assignment", e);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a calendar assignment", description = "Update an existing calendar assignment and its associated form nodes.")
    public ResponseResult<CalendarAssignmentDTO> update(@PathVariable Long id, @RequestBody CalendarAssignmentDTO dto) {
        try {
            CalendarAssignmentDTO updated = service.updateAssignment(id, dto);
            logger.info("Updated Calendar Assignment with ID: {}", id);
            return ResponseResult.success(updated);
        } catch (Exception e) {
            logger.error("Error updating calendar assignment with ID: {}", id, e);
            return ResponseResult.fail("Failed to update calendar assignment", e);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete (soft delete) a calendar assignment", description = "Soft delete a calendar assignment by setting its status to 0.")
    public ResponseResult<String> delete(@PathVariable Long id, @PathVariable Integer userId) {
        try {
            service.deleteAssignment(id, userId);
            logger.info("Soft deleted Calendar Assignment with ID: {}", id);
            return ResponseResult.success("Calendar assignment deleted successfully.");
        } catch (Exception e) {
            logger.error("Error deleting calendar assignment with ID: {}", id, e);
            return ResponseResult.fail("Failed to delete calendar assignment", e);
        }
    }

    @GetMapping
    @Operation(summary = "Get all active calendar assignments", description = "Retrieve all calendar assignments with active status (status = 1).")
    public ResponseResult<List<CalendarAssignmentDTO>> getAll() {
        try {
            List<CalendarAssignmentDTO> assignments = service.getAllAssignments();
            logger.info("Retrieved all active calendar assignments, count: {}", assignments.size());
            return assignments.isEmpty()
                    ? ResponseResult.noContent(assignments)
                    : ResponseResult.success(assignments);
        } catch (Exception e) {
            logger.error("Error retrieving all calendar assignments", e);
            return ResponseResult.fail("Failed to retrieve all calendar assignments", e);
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a calendar assignment by ID", description = "Retrieve a calendar assignment by its ID if it is active (status = 1).")
    public ResponseResult<CalendarAssignmentDTO> getById(@PathVariable Long id) {
        try {
            CalendarAssignmentDTO assignment = service.getAssignmentById(id);
            logger.info("Retrieved Calendar Assignment with ID: {}", id);
            return ResponseResult.success(assignment);
        } catch (Exception e) {
            logger.error("Error retrieving calendar assignment with ID: {}", id, e);
            return ResponseResult.fail("Failed to retrieve calendar assignment", e);
        }
    }
}
