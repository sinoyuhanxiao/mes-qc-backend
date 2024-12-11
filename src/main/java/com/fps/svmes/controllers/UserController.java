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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User API", description = "API for User")
public class UserController {


    public final UserService userService;

    public static Logger logger = LoggerFactory.getLogger(UserController.class);

    @GetMapping("")
    @Operation(summary = "Get all users", description = "Returns all users in the qc system")
    public ResponseResult<List<UserDTO>> getAllUsers() {
        try {
            List<UserDTO> personnelDTOList = userService.getAllUsers();
            logger.info("Users retrieved, count: {}", personnelDTOList.size());
            return ResponseResult.success(personnelDTOList);
        } catch (Exception e) {
            return ResponseResult.fail("Error retrieving qc users", e);
        }
    }
}
