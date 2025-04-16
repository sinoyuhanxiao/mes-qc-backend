package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.user.TeamDTO;
import com.fps.svmes.dto.requests.TeamRequest;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.services.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teams")
@Tag(name = "Team Management API", description = "API for managing teams")
public class TeamController {

    @Autowired
    private TeamService teamService;

    private static final Logger logger = LoggerFactory.getLogger(TeamController.class);

    /**
     * Create a new team.
     *
     * @param createdBy   ID of the user creating the team
     * @return TeamDTO
     */
    @PostMapping
    @Operation(summary = "Create a new team", description = "Create a new team with the provided details")
    public ResponseResult<TeamDTO> createTeam(@RequestBody TeamRequest TeamRequest, @RequestParam Integer createdBy) {
        try {
            TeamDTO createdTeam = teamService.createTeam(TeamRequest, createdBy);
            return ResponseResult.success(createdTeam);
        } catch (Exception e) {
            logger.error("Error creating team", e);
            return ResponseResult.fail("Failed to create team", e);
        }
    }

    /**
     * Update an existing team.
     *
     * @param id          ID of the team to update
     * @param teamRequest Team request object containing updated data
     * @param updatedBy   ID of the user updating the team
     * @return TeamDTO
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing team", description = "Update a team with the provided details")
    public ResponseResult<TeamDTO> updateTeam(
            @PathVariable Integer id,
            @RequestBody @Valid TeamRequest teamRequest,
            @RequestParam Integer updatedBy
    ) {
        try {
            // Delegate the update logic to the service
            TeamDTO updatedTeam = teamService.updateTeam(id, teamRequest, updatedBy);

            // Return a success response with the updated DTO
            return ResponseResult.success(updatedTeam);
        } catch (Exception e) {
            logger.error("Error updating team", e);

            // Return a failure response with the error message
            return ResponseResult.fail("Failed to update team", e);
        }
    }



    /**
     * Get a specific team by ID.
     *
     * @param id Team ID
     * @return TeamDTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a specific team by ID", description = "Fetch a team by its ID")
    public ResponseResult<TeamDTO> getTeamById(@PathVariable Integer id) {
        try {
            TeamDTO team = teamService.getTeamById(id);
            return ResponseResult.success(team);
        } catch (Exception e) {
            logger.error("Error retrieving team by ID", e);
            return ResponseResult.fail("Failed to retrieve team", e);
        }
    }

    /**
     * Get all teams.
     *
     * @return List of TeamDTO
     */
    @GetMapping
    @Operation(summary = "Get all teams", description = "Fetch all teams")
    public ResponseResult<List<TeamDTO>> getAllTeams() {
        try {
            List<TeamDTO> teams = teamService.getAllTeams();
            return ResponseResult.success(teams);
        } catch (Exception e) {
            logger.error("Error retrieving all teams", e);
            return ResponseResult.fail("Failed to retrieve teams", e);
        }
    }

    /**
     * Soft delete a team.
     *
     * @param id     ID of the team to soft delete
     * @param updatedBy ID of the user performing the action
     * @return Void
     */
    @PutMapping("/deactivate/{id}")
    @Operation(summary = "Soft delete a team", description = "Mark a team as inactive by performing a soft delete")
    public ResponseResult<Void> softDeleteTeam(@PathVariable Integer id, @RequestParam Integer updatedBy) {
        try {
            teamService.softDeleteTeam(id, updatedBy);
            return ResponseResult.success();
        } catch (Exception e) {
            logger.error("Error soft deleting team", e);
            return ResponseResult.fail("Failed to soft delete team", e);
        }
    }

    /**
     * Activate an inactive team.
     *
     * @param id        ID of the team to activate
     * @param updatedBy ID of the user performing the action
     * @return Void
     */
    @PutMapping("/activate/{id}")
    @Operation(summary = "Activate an inactive team", description = "Mark an inactive team as active")
    public ResponseResult<Void> activateTeam(@PathVariable Integer id, @RequestParam Integer updatedBy) {
        try {
            teamService.activateTeam(id, updatedBy);
            return ResponseResult.success();
        } catch (Exception e) {
            logger.error("Error activating team", e);
            return ResponseResult.fail("Failed to activate team", e);
        }
    }

    /**
     * Hard delete a team.
     *
     * @param id ID of the team to hard delete
     * @return Void
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Hard delete a team", description = "Permanently delete a team")
    public ResponseResult<Void> hardDeleteTeam(@PathVariable Integer id) {
        try {
            teamService.hardDeleteTeam(id);
            return ResponseResult.success();
        } catch (Exception e) {
            logger.error("Error hard deleting team", e);
            return ResponseResult.fail("Failed to hard delete team", e);
        }
    }
}
