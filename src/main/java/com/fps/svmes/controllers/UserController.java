package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.user.UserDTO;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.dto.responses.ResponseStatus;
import com.fps.svmes.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.Inet4Address;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User API", description = "API for User")
public class UserController {

    public final UserService userService;
    public static Logger logger = LoggerFactory.getLogger(UserController.class);

    // GET - Retrieve all users
    @GetMapping("")
    @Operation(summary = "Get all users", description = "Returns all users in the QC system")
    public ResponseResult<List<UserDTO>> getAllUsers() {
        try {
            List<UserDTO> userDTOList = userService.getAllUsers();
            logger.info("Users retrieved, count: {}", userDTOList.size());
            return ResponseResult.success(userDTOList);
        } catch (Exception e) {
            logger.error("Error retrieving users", e);
            return ResponseResult.fail("Error retrieving QC users", e);
        }
    }

    // POST - Create a new user
    @PostMapping("")
    @Operation(summary = "Create a new user", description = "Creates a new user in the QC system")
    public ResponseResult<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        try {
            UserDTO createdUser = userService.createUser(userDTO);
            logger.info("User created: {}", createdUser);
            return ResponseResult.success(createdUser);
        } catch (Exception e) {
            logger.error("Error creating user", e);
            return ResponseResult.fail("Error creating QC user", e);
        }
    }

    // PUT - Update an existing user
    @PutMapping("/{id}")
    @Operation(summary = "Update a user", description = "Updates a user's information in the QC system")
    public ResponseResult<UserDTO> updateUser(@PathVariable Integer id, @RequestBody UserDTO userDTO) {
        try {
            UserDTO updatedUser = userService.updateUser(id, userDTO);
            logger.info("User updated: {}", updatedUser);
            return ResponseResult.success(updatedUser);
        } catch (Exception e) {
            logger.error("Error updating user with ID: {}", id, e);
            return ResponseResult.fail("Error updating QC user", e);
        }
    }

    // DELETE - Delete a user by ID
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user", description = "Deletes a user from the QC system")
    public ResponseResult<Void> deleteUser(@PathVariable Integer id) {
        try {
            userService.deleteUser(id);
            logger.info("User deleted with ID: {}", id);
            return ResponseResult.success(null);
        } catch (Exception e) {
            logger.error("Error deleting user with ID: {}", id, e);
            return ResponseResult.fail("Error deleting QC user", e);
        }
    }
}
