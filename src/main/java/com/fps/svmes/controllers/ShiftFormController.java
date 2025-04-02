package com.fps.svmes.controllers;

import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.services.ShiftFormService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/shift-forms")
@RequiredArgsConstructor
@Tag(name = "Shift-Form API", description = "API for managing shift-form relationships")
public class ShiftFormController {
    private final ShiftFormService shiftFormService;
    public static final Logger logger = LoggerFactory.getLogger(ShiftFormController.class);

    @PostMapping("/shifts/{shiftId}/forms")
    @Operation(summary = "Assign multiple forms to a shift", description = "Assign multiple forms to a single shift")
    public ResponseResult<String> assignFormsToShift(@PathVariable Integer shiftId, @RequestBody List<String> formIds) {
        try {
            for (String formId: formIds) {
                shiftFormService.assignFormToShift(shiftId, formId);
            }
            logger.info("Forms {} assigned to shift {}", formIds, shiftId);
            return ResponseResult.success(null);
        } catch (Exception e) {
            logger.error("Error assigning forms {} to shift {}", formIds, shiftId, e);
            return ResponseResult.fail("Error assigning forms to shift", e);
        }
    }

    @DeleteMapping("/shifts/{shiftId}/forms/")
    @Operation(summary = "Remove a form from a shift", description = "Unassign a specific form from a specific shift")
    public ResponseResult<String> removeFormFromShift(@PathVariable Integer shiftId, @RequestBody List<String> formIds) {
        try {
            for (String formId: formIds) {
                shiftFormService.removeFormFromShift(shiftId, formId);
                logger.info("Form {} removed from shift {}", formId, shiftId);
            }
            return ResponseResult.success(null);
        } catch (Exception e) {
            logger.error("Error removing forms {} to shift {}", formIds, shiftId, e);
            return ResponseResult.fail("Error removing form from shift", e);
        }
    }

    @DeleteMapping("/shifts/{shiftId}/forms")
    @Operation(summary = "Remove all forms from a shift", description = "Unassign all forms from a specific shift")
    public ResponseResult<Void> removeAllFormsFromShift(@PathVariable Integer shiftId) {
        try {
            shiftFormService.removeAllFormsFromShift(shiftId);
            logger.info("All forms removed from shift {}", shiftId);
            return ResponseResult.success(null);
        } catch (Exception e) {
            logger.error("Error removing all forms from shift {}", shiftId, e);
            return ResponseResult.fail("Error removing all forms from shift", e);
        }
    }

    @GetMapping("/shifts/{shiftId}/forms")
    @Operation(summary = "Get forms assigned to shift", description = "Retrieve all form IDs assigned to a specific shift")
    public ResponseResult<List<String>> getFormIdsForShift(@PathVariable Integer shiftId) {
        try {
            List<String> formIds = shiftFormService.getFormIdsByShift(shiftId);
            logger.info("Forms for shift {} retrieved: {}", shiftId, formIds);
            return ResponseResult.success(formIds);
        } catch (Exception e) {
            logger.error("Error retrieving forms for shift {}", shiftId, e);
            return ResponseResult.fail("Error retrieving forms for shift", e);
        }
    }
}