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
import org.springframework.web.bind.annotation.*;

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

    // POST - Validate username and password
    @PostMapping("/validate")
    @Operation(summary = "Validate User Credentials", description = "Validates the provided username and password")
    public ResponseResult<String> validateUser(@RequestParam String username, @RequestParam String password) {
        try {
            boolean isValid = userService.validateCredentials(username, password);

            if (isValid) {
                logger.info("User validation successful for username: {}", username);
                return ResponseResult.success("User validated successfully");
            } else {
                logger.warn("Invalid credentials for username: {}", username);
                return ResponseResult.fail("Invalid username or password");
            }
        } catch (Exception e) {
            logger.error("Error validating user credentials", e);
            return ResponseResult.fail("Error validating user credentials", e);
        }
    }

    // New Method - Get User Information by Username
    @GetMapping("/info")
    @Operation(summary = "Get User Information", description = "Fetches user information by username")
    public ResponseResult<UserDTO> getUserByUsername(@RequestParam String username) {
        try {
            UserDTO userDTO = userService.getUserByUsername(username);

            if (userDTO != null) {
                logger.info("User information retrieved for username: {}", username);
                return ResponseResult.success(userDTO);
            } else {
                logger.warn("No user found with username: {}", username);
                return ResponseResult.fail("User not found");
            }
        } catch (Exception e) {
            logger.error("Error retrieving user information for username: {}", username, e);
            return ResponseResult.fail("Error retrieving user information", e);
        }
    }


    // Get User Information by Id
    @GetMapping("/{id}")
    @Operation(summary = "Get User Information By Id", description = "Fetches user information by id")
    public ResponseResult<UserDTO> getUserById(@RequestParam Integer id) {
        try {
            UserDTO userDTO = userService.getUserById(id);

            if (userDTO != null) {
                logger.info("User information retrieved for id: {}", id);
                return ResponseResult.success(userDTO);
            } else {
                logger.warn("No user found with id: {}", id);
                return ResponseResult.fail("User not found");
            }
        } catch (Exception e) {
            logger.error("Error retrieving user information for id: {}", id, e);
            return ResponseResult.fail("Error retrieving user information", e);
        }
    }


}
