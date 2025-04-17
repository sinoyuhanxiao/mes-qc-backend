package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.user.TeamDTO;
import com.fps.svmes.dto.requests.TeamRequest;
import jakarta.validation.Valid;

import java.util.List;

public interface TeamService {
    TeamDTO createTeam(TeamRequest TeamRequest, Integer userId);
    TeamDTO updateTeam(Integer id, @Valid TeamRequest teamDTO, Integer userId);
    TeamDTO getTeamById(Integer id);
    List<TeamDTO> getAllTeams();
    void activateTeam(Integer id, Integer updatedBy);
    void softDeleteTeam(Integer id, Integer userId);
    void hardDeleteTeam(Integer id);
    TeamDTO getTeamByTeamLeadId(Integer id);
    List<Integer> getCurrentLeaderIds();
}