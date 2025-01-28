package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.user.ShiftUserDTO;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.services.ShiftUserService;
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
@Slf4j
@RequestMapping("/shift-users")
@RequiredArgsConstructor
@Tag(name = "Shift-User API", description = "API for managing Shift-User relationships")
public class ShiftUserController {

    @Autowired
    private final ShiftUserService shiftUserService;

    public static Logger logger = LoggerFactory.getLogger(ShiftUserController.class);

    @PostMapping("/users/{userId}/shifts")
    @Operation(summary = "Assign user to multiple shifts", description = "Assign a single user to multiple shifts")
    public ResponseResult<Void> assignUserToShifts(@PathVariable Long userId, @RequestBody List<Long> shiftIds) {
        try {
            shiftUserService.assignUserToShifts(userId, shiftIds);
            logger.info("User {} assigned to shifts {}", userId, shiftIds);
            return ResponseResult.success(null);
        } catch (Exception e) {
            logger.error("Error assigning user {} to shifts {}", userId, shiftIds, e);
            return ResponseResult.fail("Error assigning user to shifts", e);
        }
    }

    @PostMapping("/shifts/{shiftId}/users")
    @Operation(summary = "Assign multiple users to a shift", description = "Assign multiple users to a single shift")
    public ResponseResult<Void> assignUsersToShift(@PathVariable Long shiftId, @RequestBody List<Long> userIds) {
        try {
            shiftUserService.assignUsersToShift(shiftId, userIds);
            logger.info("Users {} assigned to shift {}", userIds, shiftId);
            return ResponseResult.success(null);
        } catch (Exception e) {
            logger.error("Error assigning users {} to shift {}", userIds, shiftId, e);
            return ResponseResult.fail("Error assigning users to shift", e);
        }
    }

    @DeleteMapping("/users/{userId}/shifts/{shiftId}")
    @Operation(summary = "Remove a user from a shift", description = "Unassign a specific user from a specific shift")
    public ResponseResult<Void> removeUserFromShift(@PathVariable Long userId, @PathVariable Long shiftId) {
        try {
            shiftUserService.removeUserFromShift(userId, shiftId);
            logger.info("User {} removed from shift {}", userId, shiftId);
            return ResponseResult.success(null);
        } catch (Exception e) {
            logger.error("Error removing user {} from shift {}", userId, shiftId, e);
            return ResponseResult.fail("Error removing user from shift", e);
        }
    }

    @DeleteMapping("/users/{userId}/shifts")
    @Operation(summary = "Remove user from all shifts", description = "Unassign a specific user from all shifts")
    public ResponseResult<Void> removeUserFromAllShifts(@PathVariable Long userId) {
        try {
            shiftUserService.removeUserFromAllShifts(userId);
            logger.info("User {} removed from all shifts", userId);
            return ResponseResult.success(null);
        } catch (Exception e) {
            logger.error("Error removing user {} from all shifts", userId, e);
            return ResponseResult.fail("Error removing user from all shifts", e);
        }
    }

    @DeleteMapping("/shifts/{shiftId}/users")
    @Operation(summary = "Remove multiple users from a shift", description = "Unassign multiple users from a specific shift")
    public ResponseResult<Void> removeUsersFromShift(@PathVariable Long shiftId, @RequestBody List<Long> userIds) {
        try {
            shiftUserService.removeUsersFromShift(shiftId, userIds);
            logger.info("Users {} removed from shift {}", userIds, shiftId);
            return ResponseResult.success(null);
        } catch (Exception e) {
            logger.error("Error removing users {} from shift {}", userIds, shiftId, e);
            return ResponseResult.fail("Error removing users from shift", e);
        }
    }

    @GetMapping("/users/{userId}/shifts")
    @Operation(summary = "Get shifts for user", description = "Retrieve all shifts assigned to a specific user")
    public ResponseResult<List<ShiftUserDTO>> getShiftsForUser(@PathVariable Long userId) {
        try {
            List<ShiftUserDTO> shifts = shiftUserService.getShiftsForUser(userId);
            logger.info("Shifts for user {} retrieved: {}", userId, shifts);
            return ResponseResult.success(shifts);
        } catch (Exception e) {
            logger.error("Error retrieving shifts for user {}", userId, e);
            return ResponseResult.fail("Error retrieving shifts for user", e);
        }
    }

    @GetMapping("/shifts/{shiftId}/users")
    @Operation(summary = "Get users for shift", description = "Retrieve all users assigned to a specific shift")
    public ResponseResult<List<ShiftUserDTO>> getUsersForShift(@PathVariable Long shiftId) {
        try {
            List<ShiftUserDTO> users = shiftUserService.getUsersForShift(shiftId);
            logger.info("Users for shift {} retrieved: {}", shiftId, users);
            return ResponseResult.success(users);
        } catch (Exception e) {
            logger.error("Error retrieving users for shift {}", shiftId, e);
            return ResponseResult.fail("Error retrieving users for shift", e);
        }
    }

    @GetMapping("")
    @Operation(summary = "Get all shift-user relationships", description = "Retrieve all shift-user assignments")
    public ResponseResult<List<ShiftUserDTO>> getAllShiftUsers() {
        try {
            List<ShiftUserDTO> allShiftUsers = shiftUserService.getAllShiftUsers();
            logger.info("All shift-user relationships retrieved: {}", allShiftUsers);
            return ResponseResult.success(allShiftUsers);
        } catch (Exception e) {
            logger.error("Error retrieving all shift-user relationships", e);
            return ResponseResult.fail("Error retrieving all shift-user relationships", e);
        }
    }

//    @PutMapping("/shifts/{shiftId}/leader")
//    @Operation(summary = "Set or update shift leader", description = "Assign or update the leader for a specific shift")
//    public ResponseResult<Void> setShiftLeader(@PathVariable Long shiftId, @RequestBody Long leaderId) {
//        try {
//            shiftUserService.setShiftLeader(shiftId, leaderId);
//            logger.info("Leader {} set for shift {}", leaderId, shiftId);
//            return ResponseResult.success(null);
//        } catch (Exception e) {
//            logger.error("Error setting leader {} for shift {}", leaderId, shiftId, e);
//            return ResponseResult.fail("Error setting shift leader", e);
//        }
//    }

//    @GetMapping("/users/{userId}/led-shifts")
//    @Operation(summary = "Get shifts led by user", description = "Retrieve all shifts where a specific user is the leader")
//    public ResponseResult<List<ShiftUserDTO>> getLedShiftsForUser(@PathVariable Long userId) {
//        try {
//            List<ShiftUserDTO> ledShifts = shiftUserService.getLedShiftsForUser(userId);
//            logger.info("Shifts led by user {} retrieved: {}", userId, ledShifts);
//            return ResponseResult.success(ledShifts);
//        } catch (Exception e) {
//            logger.error("Error retrieving shifts led by user {}", userId, e);
//            return ResponseResult.fail("Error retrieving shifts led by user", e);
//        }
//    }
}