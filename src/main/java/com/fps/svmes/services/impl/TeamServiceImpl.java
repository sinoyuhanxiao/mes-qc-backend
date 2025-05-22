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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import com.fps.svmes.models.sql.user.User;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamServiceImpl implements TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    @Transactional
    public TeamDTO createTeam(TeamRequest teamRequest) {
        // Map the TeamRequest to the Team entity
        Team team = modelMapper.map(teamRequest, Team.class);

        team.setParent(resolveParent(teamRequest.getParentId()));

        if (teamRequest.getCreatedBy() != null) {
            team.setCreationDetails(teamRequest.getCreatedBy(), 1);
        }

        // Set the leader based on leaderId
        if (teamRequest.getLeaderId() != null) {
            User leader = userRepository.findById(teamRequest.getLeaderId())
                    .orElseThrow(() -> new IllegalArgumentException("Leader not found with ID: " + teamRequest.getLeaderId()));

            // If leader is already assigned to another team, change that team's leader to null
            Team other = teamRepository.findByLeaderId(teamRequest.getLeaderId());
            if (other != null) {
                other.setLeader(null);
                other.setUpdateDetails(teamRequest.getCreatedBy(), other.getStatus());
            }

            team.setLeader(leader);
        }

        // Save the Team entity
        team = teamRepository.save(team);

        // Map the saved Team entity to the TeamDTO
        return modelMapper.map(team, TeamDTO.class);
    }

    @Override
    @Transactional
    public TeamDTO updateTeam(Integer id, @Valid TeamRequest teamRequest) {
        Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));

        // Map only non-null properties from DTO to entity
        if (teamRequest.getName() != null) team.setName(teamRequest.getName());
        if (teamRequest.getType() != null) team.setType(teamRequest.getType());
        if (teamRequest.getStartTime() != null) team.setStartTime(teamRequest.getStartTime());
        if (teamRequest.getEndTime() != null) team.setEndTime(teamRequest.getEndTime());
        if (teamRequest.getDescription() != null) team.setDescription(teamRequest.getDescription());
        if (teamRequest.getParentId() != null) team.setParent(resolveParent(teamRequest.getParentId()));
        if (teamRequest.getStatus() != null) team.setStatus(teamRequest.getStatus()); // Soft delete
        if (teamRequest.getUpdatedBy() != null) team.setUpdateDetails(teamRequest.getUpdatedBy(),1);

        if (teamRequest.getLeaderId() != null) {
            User newLeader = userRepository.findById(teamRequest.getLeaderId())
                    .orElseThrow(() -> new RuntimeException("Leader not found"));

            // Current (old) leader of this team – may be null
            User oldLeader = team.getLeader();

            // Other team that currently has this new leader
            Team otherTeam = teamRepository.findByLeaderId(newLeader.getId());

            if (otherTeam != null && !otherTeam.getId().equals(team.getId())) {
                otherTeam.setLeader(oldLeader);

                if (teamRequest.getUpdatedBy() != null) {
                    otherTeam.setUpdateDetails(teamRequest.getUpdatedBy(), otherTeam.getStatus());
                }
                // Hibernate will flush otherTeam because it's in the same persistence context
                team.setLeader(newLeader);
            }
        } else {
            team.setLeader(null);
        }

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

    @Transactional(readOnly=true)
    public List<TeamDTO> getTeamTree() {
        List<Team> roots = teamRepository.findByParentIsNull();
        return roots.stream().map(this::mapWithChildren).toList();
    }

    @Override
    @Transactional
    public void softDeleteTeam(Integer id, Integer userId) {
        Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));
        softDeleteRecursively(team, userId);
        teamRepository.save(team);
    }

    private void softDeleteRecursively(Team node, Integer userId) {
        node.setUpdateDetails(userId, 0);

        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            node.getChildren().forEach(child -> softDeleteRecursively(child, userId));
        }
    }

    @Override
    public void hardDeleteTeam(Integer id) {
        if (!teamRepository.existsById(id)) {
            throw new RuntimeException("Team not found");
        }
        teamRepository.deleteById(id); // Permanently deletes the record
    }

    @Override
    @Transactional
    public void activateTeam(Integer id, Integer updatedBy) {
        Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));

        if (team.getStatus() == 1) {
            throw new RuntimeException("Team is already active");
        }

        // Rule: allowed if (a) root team OR (b) parent exists AND is active
        if (team.getParent() != null && team.getParent().getStatus() != 1) {
            throw new RuntimeException("Cannot activate team: parent team is inactive");
        }

        team.setUpdateDetails(updatedBy, 1);
        teamRepository.save(team);
    }

    @Override
    public TeamDTO getTeamByTeamLeadId(Integer id) {
        Team team = teamRepository.findByLeaderId(id);
        return modelMapper.map(team, TeamDTO.class);
    }

    @Override
    public List<Integer> getCurrentLeaderIds() {
        return teamRepository.findAll()
                .stream()
                .map(Team::getLeader)          // User or null
                .filter(Objects::nonNull)      // keep only teams that have a leader
                .map(User::getId)              // convert User → Integer
                .distinct()                    // in case the same leader heads >1 team
                .collect(Collectors.toList());
    }

    /**
     * Returns the depth of the team in the hierarchy.
     * root           → 1
     * child of root  → 2
     * grand‑child    → 3 …
     */
    @Override
    @Transactional(readOnly = true)
    public int getDepth(Integer teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found: " + teamId));

        int depth = 1;
        Team cur  = team;

        while (cur.getParent() != null) {
            depth++;
            cur = cur.getParent();
        }
        return depth;
    }

    private Team resolveParent(Integer parentId) {
        if (parentId == null) return null;
        return teamRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent team not found: " + parentId));
    }

    private TeamDTO mapWithChildren(Team entity) {
        TeamDTO dto = modelMapper.map(entity, TeamDTO.class);
        if (entity.getParent() != null){
            dto.setParentId(entity.getParent().getId());
        }

        if (entity.getChildren() != null && !entity.getChildren().isEmpty()) {
            dto.setChildren(entity.getChildren().stream()
                    .map(this::mapWithChildren)
                    .toList());
        }
        return dto;
    }
}

