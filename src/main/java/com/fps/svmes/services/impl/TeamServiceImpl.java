package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.user.TeamDTO;
import com.fps.svmes.dto.requests.TeamRequest;
import com.fps.svmes.models.sql.user.Team;
import com.fps.svmes.repositories.jpaRepo.user.TeamRepository;
import com.fps.svmes.services.TeamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.modelmapper.ModelMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import com.fps.svmes.models.sql.user.User;

@Service
public class TeamServiceImpl implements TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public TeamDTO createTeam(TeamRequest teamRequest, Integer userId) {
        // Map the TeamRequest to the Team entity
        Team team = modelMapper.map(teamRequest, Team.class);

        // Set audit fields
        OffsetDateTime now = OffsetDateTime.now();
        team.setCreatedAt(now);
        team.setCreatedBy(userId);
        team.setUpdatedAt(now);
        team.setUpdatedBy(userId);

        // Set the leader based on leaderId
        if (teamRequest.getLeaderId() != null) {
            User leader = userRepository.findById(teamRequest.getLeaderId())
                    .orElseThrow(() -> new IllegalArgumentException("Leader not found with ID: " + teamRequest.getLeaderId()));
            team.setLeader(leader);
        }

        // Save the Team entity
        team = teamRepository.save(team);

        // Map the saved Team entity to the TeamDTO
        return modelMapper.map(team, TeamDTO.class);
    }

    @Override
    public TeamDTO updateTeam(Integer id, @Valid TeamRequest teamRequest, Integer userId) {
        Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));

        // Map only non-null properties from DTO to entity
        if (teamRequest.getName() != null) team.setName(teamRequest.getName());
        if (teamRequest.getType() != null) team.setType(teamRequest.getType());
        if (teamRequest.getLeaderId() != null) {
            User leader = userRepository.findById(teamRequest.getLeaderId())
                    .orElseThrow(() -> new RuntimeException("Leader not found"));
            team.setLeader(leader);
        }
        if (teamRequest.getStartTime() != null) team.setStartTime(teamRequest.getStartTime());
        if (teamRequest.getEndTime() != null) team.setEndTime(teamRequest.getEndTime());
        if (teamRequest.getDescription() != null) team.setDescription(teamRequest.getDescription());

        if (teamRequest.getStatus() != null) {
            team.setStatus(teamRequest.getStatus()); // Soft delete
        }

        team.setUpdatedAt(OffsetDateTime.now());
        team.setUpdatedBy(userId);

        team = teamRepository.save(team);
        return modelMapper.map(team, TeamDTO.class);
    }

    @Override
    public TeamDTO getTeamById(Integer id) {
        Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));
        return modelMapper.map(team, TeamDTO.class);
    }

    @Override
    public List<TeamDTO> getAllTeams() {
        List<Team> teams = teamRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        return teams.stream()
                .map(team -> modelMapper.map(team, TeamDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void softDeleteTeam(Integer id, Integer userId) {
        Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));
        team.setStatus(0); // Soft delete
        team.setUpdatedAt(OffsetDateTime.now());
        team.setUpdatedBy(userId);
        teamRepository.save(team);
    }

    @Override
    public void hardDeleteTeam(Integer id) {
        if (!teamRepository.existsById(id)) {
            throw new RuntimeException("Team not found");
        }
        teamRepository.deleteById(id); // Permanently deletes the record
    }


    @Override
    public void activateTeam(Integer id, Integer updatedBy) {
        Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));

        if (team.getStatus() == 1) {
            throw new RuntimeException("Team is already active");
        }

        team.setStatus(1); // Reactivate the team
        team.setUpdatedAt(OffsetDateTime.now());
        team.setUpdatedBy(updatedBy);

        teamRepository.save(team);
    }

    @Override
    public TeamDTO getTeamByTeamLeadId(Integer id) {
        Team team = teamRepository.findByLeaderId(id);
        return modelMapper.map(team, TeamDTO.class);
    }

    @Override
    public List<Integer> getCurrentLeaderIds() {
        // Fetch all teams
        List<Team> teams = teamRepository.findAll();
        // Extract leader IDs from teams
        return teams.stream()
                .map(team -> team.getLeader().getId())
                .collect(Collectors.toList());
    }

}

