package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.user.ShiftDTO;
import com.fps.svmes.dto.requests.ShiftRequest;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.models.sql.user.Shift;
import com.fps.svmes.services.ShiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shifts")
@Tag(name = "Shift Management API", description = "API for managing shifts")
public class ShiftController {

    @Autowired
    private ShiftService shiftService;

    private static final Logger logger = LoggerFactory.getLogger(ShiftController.class);

    /**
     * Create a new shift.
     *
     * @param shiftDTO Shift data transfer object
     * @param createdBy   ID of the user creating the shift
     * @return ShiftDTO
     */
    @PostMapping
    @Operation(summary = "Create a new shift", description = "Create a new shift with the provided details")
    public ResponseResult<ShiftDTO> createShift(@RequestBody ShiftDTO shiftDTO, @RequestParam Integer createdBy) {
        try {
            ShiftDTO createdShift = shiftService.createShift(shiftDTO, createdBy);
            return ResponseResult.success(createdShift);
        } catch (Exception e) {
            logger.error("Error creating shift", e);
            return ResponseResult.fail("Failed to create shift", e);
        }
    }

    /**
     * Update an existing shift.
     *
     * @param id          ID of the shift to update
     * @param shiftRequest Shift request object containing updated data
     * @param updatedBy   ID of the user updating the shift
     * @return ShiftDTO
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing shift", description = "Update a shift with the provided details")
    public ResponseResult<ShiftDTO> updateShift(
            @PathVariable Integer id,
            @RequestBody @Valid ShiftRequest shiftRequest,
            @RequestParam Integer updatedBy
    ) {
        try {
            // Delegate the update logic to the service
            ShiftDTO updatedShift = shiftService.updateShift(id, shiftRequest, updatedBy);

            // Return a success response with the updated DTO
            return ResponseResult.success(updatedShift);
        } catch (Exception e) {
            logger.error("Error updating shift", e);

            // Return a failure response with the error message
            return ResponseResult.fail("Failed to update shift", e);
        }
    }



    /**
     * Get a specific shift by ID.
     *
     * @param id Shift ID
     * @return ShiftDTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a specific shift by ID", description = "Fetch a shift by its ID")
    public ResponseResult<ShiftDTO> getShiftById(@PathVariable Integer id) {
        try {
            ShiftDTO shift = shiftService.getShiftById(id);
            return ResponseResult.success(shift);
        } catch (Exception e) {
            logger.error("Error retrieving shift by ID", e);
            return ResponseResult.fail("Failed to retrieve shift", e);
        }
    }

    /**
     * Get all shifts.
     *
     * @return List of ShiftDTO
     */
    @GetMapping
    @Operation(summary = "Get all shifts", description = "Fetch all shifts")
    public ResponseResult<List<ShiftDTO>> getAllShifts() {
        try {
            List<ShiftDTO> shifts = shiftService.getAllShifts();
            return ResponseResult.success(shifts);
        } catch (Exception e) {
            logger.error("Error retrieving all shifts", e);
            return ResponseResult.fail("Failed to retrieve shifts", e);
        }
    }

    /**
     * Soft delete a shift.
     *
     * @param id     ID of the shift to soft delete
     * @param updatedBy ID of the user performing the action
     * @return Void
     */
    @PutMapping("/deactivate/{id}")
    @Operation(summary = "Soft delete a shift", description = "Mark a shift as inactive by performing a soft delete")
    public ResponseResult<Void> softDeleteShift(@PathVariable Integer id, @RequestParam Integer updatedBy) {
        try {
            shiftService.softDeleteShift(id, updatedBy);
            return ResponseResult.success();
        } catch (Exception e) {
            logger.error("Error soft deleting shift", e);
            return ResponseResult.fail("Failed to soft delete shift", e);
        }
    }

    /**
     * Activate an inactive shift.
     *
     * @param id        ID of the shift to activate
     * @param updatedBy ID of the user performing the action
     * @return Void
     */
    @PutMapping("/activate/{id}")
    @Operation(summary = "Activate an inactive shift", description = "Mark an inactive shift as active")
    public ResponseResult<Void> activateShift(@PathVariable Integer id, @RequestParam Integer updatedBy) {
        try {
            shiftService.activateShift(id, updatedBy);
            return ResponseResult.success();
        } catch (Exception e) {
            logger.error("Error activating shift", e);
            return ResponseResult.fail("Failed to activate shift", e);
        }
    }

    /**
     * Hard delete a shift.
     *
     * @param id ID of the shift to hard delete
     * @return Void
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Hard delete a shift", description = "Permanently delete a shift")
    public ResponseResult<Void> hardDeleteShift(@PathVariable Integer id) {
        try {
            shiftService.hardDeleteShift(id);
            return ResponseResult.success();
        } catch (Exception e) {
            logger.error("Error hard deleting shift", e);
            return ResponseResult.fail("Failed to hard delete shift", e);
        }
    }
}
