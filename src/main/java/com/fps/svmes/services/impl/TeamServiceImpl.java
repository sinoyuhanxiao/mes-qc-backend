package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.user.LeaderDTO;
import com.fps.svmes.dto.dtos.user.TeamDTO;
import com.fps.svmes.dto.requests.TeamRequest;
import com.fps.svmes.models.sql.user.Team;
import com.fps.svmes.repositories.jpaRepo.user.TeamFormRepository;
import com.fps.svmes.repositories.jpaRepo.user.TeamRepository;
import com.fps.svmes.repositories.jpaRepo.user.TeamUserRepository;
import com.fps.svmes.services.TeamFormService;
import com.fps.svmes.services.TeamService;
import com.fps.svmes.services.TeamUserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.modelmapper.ModelMapper;

import java.util.*;

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
    private TeamUserRepository teamUserRepository;

    @Autowired
    private TeamFormRepository teamFormRepository;

    @Autowired
    private TeamUserService teamUserService;

    @Autowired
    private TeamFormService teamFormService;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    @Transactional
    public TeamDTO createTeam(TeamRequest teamRequest) {
        Team team = modelMapper.map(teamRequest, Team.class);

        if (teamRequest.getCreatedBy() != null) {
            team.setCreationDetails(teamRequest.getCreatedBy(), teamRequest.getStatus());
        }

        if (teamRequest.getParentId() != null) {
            Team parentTeam = teamRepository.findById(teamRequest.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent team not found: " + teamRequest.getParentId()));
            team.setParent(parentTeam);
        }

        if (teamRequest.getLeaderId() != null) {
            User leader = userRepository.findById(teamRequest.getLeaderId())
                    .orElseThrow(() -> new IllegalArgumentException("Leader not found with ID: " + teamRequest.getLeaderId()));

            // If leader is already assigned to another team, change that team's leader to null
            Team other = teamRepository.findByLeaderId(teamRequest.getLeaderId());


            if (other != null) {
                other.setLeader(null);

                // Remove the leader member since that leader is no longer within the team
                teamUserService.removeUserFromTeam(leader.getId(), other.getId());

                other.setUpdateDetails(teamRequest.getCreatedBy(), other.getStatus());
            }

            team.setLeader(leader);
        }

        team = teamRepository.save(team);
        return modelMapper.map(team, TeamDTO.class);
    }

    @Override
    @Transactional
    public TeamDTO updateTeam(Integer id, @Valid TeamRequest teamRequest) {
        Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));

        // Map only non-null properties from request to team entity
        if (teamRequest.getName() != null) team.setName(teamRequest.getName());
        if (teamRequest.getType() != null) team.setType(teamRequest.getType());
        if (teamRequest.getStartTime() != null) team.setStartTime(teamRequest.getStartTime());
        if (teamRequest.getEndTime() != null) team.setEndTime(teamRequest.getEndTime());
        if (teamRequest.getDescription() != null) team.setDescription(teamRequest.getDescription());
        if (teamRequest.getUpdatedBy() != null) team.setUpdateDetails(teamRequest.getUpdatedBy(),teamRequest.getStatus());

        if (teamRequest.getParentId() != null) {
            Team parentTeam = teamRepository.findById(teamRequest.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent team not found: " + teamRequest.getParentId()));
            team.setParent(parentTeam);
        } else {
            team.setParent(null);
        }

        if (teamRequest.getLeaderId() != null) {
            User newLeader = userRepository.findById(teamRequest.getLeaderId())
                    .orElseThrow(() -> new RuntimeException("Leader not found"));

            // Current (old) leader of this team – may be null
            User oldLeader = team.getLeader();

            // Other team that currently has this new leader - may be null
            Team otherTeam = teamRepository.findByLeaderId(newLeader.getId());

            if (Objects.equals(oldLeader, newLeader)) {
                // Skip since same leader already assigned
            } else if (otherTeam == null || otherTeam.getId().equals(team.getId())) {
                team.setLeader(newLeader);
            } else {
                otherTeam.setLeader(oldLeader);

                // Remove previous leader from the member list
                teamUserService.removeUserFromTeam(newLeader.getId(), otherTeam.getId());

                // Add leader as a member to other team
                teamUserService.assignUserToTeams(oldLeader.getId(), List.of(otherTeam.getId()));

                if (teamRequest.getUpdatedBy() != null) {
                    otherTeam.setUpdateDetails(teamRequest.getUpdatedBy(), otherTeam.getStatus());
                }

                team.setLeader(newLeader);
            }
        } else {
            team.setLeader(null);
        }

        team = teamRepository.save(team);
        return modelMapper.map(team, TeamDTO.class);
    }

    @Override
    @Transactional
    public TeamDTO getTeamById(Integer id) {
        Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));
        List<Integer> ids = teamRepository.findSelfAndAncestorIds(id);
        return mapWithChildren(team, ids.size());
    }

    @Override
    @Transactional(readOnly=true)
    public List<TeamDTO> getFullTeamTree() {
        List<Team> roots = teamRepository.findByParentIsNull();
        return roots.stream()
                .sorted(Comparator.comparing(Team::getId))
                .map(t -> mapWithChildren(t,1))
                .toList();
    }

    @Override
    @Transactional
    public void softDeleteTeam(Integer id, Integer userId) {
        Team team = teamRepository.findById(id).orElseThrow(() -> new RuntimeException("Team not found"));
        softDeleteRecursively(team, userId);
        teamRepository.save(team);
    }

    // Helper to soft-delete a team and all descendant teams of it
    private void softDeleteRecursively(Team team, Integer userId) {
        team.setUpdateDetails(userId, 0);

        if (team.getChildren() != null && !team.getChildren().isEmpty()) {
            team.getChildren().forEach(child -> softDeleteRecursively(child, userId));
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

        // Prevent activating a team when it has a parent team and parent team is not activated
        if (team.getParent() != null && team.getParent().getStatus() != 1) {
            throw new RuntimeException("Cannot activate team: parent team is inactive");
        }

        team.setUpdateDetails(updatedBy, 1);
        teamRepository.save(team);
    }

    @Override
    @Transactional
    public TeamDTO getTeamByTeamLeadId(Integer id) {
        Team team = teamRepository.findByLeaderId(id);

        if (team == null) {
            throw new EntityNotFoundException("No team found for leader ID: " + id);
        }

        return modelMapper.map(team, TeamDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderDTO> getCurrentLeaders() {
        return teamRepository.findAll()
                .stream()
                .filter(team -> team.getLeader() != null)      // Keep only teams that have a leader
                .map(team ->  new LeaderDTO(team.getLeader().getId(),
                        team.getLeader().getName(),
                        team.getId(),
                        team.getName()))
                .toList();
    }

    /**
     * Returns the depth of the target team specified by team id in the hierarchy.
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
        Team current  = team;

        while (current.getParent() != null) {
            depth++;
            current = current.getParent();
        }

        return depth;
    }

    @Override
    @Transactional
    public void syncSelfAndDescendantTeamMembers(Integer teamId, List<Integer> parentAllowedUserIds) {
        Set<Integer> allowedSet = new HashSet<>(parentAllowedUserIds);

        List<Integer> childTeamIds = descendantTeamIds(teamId);
        if (childTeamIds.isEmpty()) return;

        for (Integer childTeamId : childTeamIds) {
            List<Integer> childUsers = teamUserRepository.findByIdTeamId(childTeamId)
                    .stream()
                    .map(teamUser -> teamUser.getUser().getId())
                    .toList();

            List<Integer> targetRemovalUserIds = childUsers.stream()
                    .filter(id -> !allowedSet.contains(id))
                    .toList();

            if (!targetRemovalUserIds.isEmpty()) {
                teamUserRepository.deleteByTeamIdAndUserIdIn(childTeamId, targetRemovalUserIds);
            }
        }
    }

    @Override
    @Transactional
    public void syncSelfAndDescendantTeamForms(Integer teamId, List<String> parentAllowedFormIds) {
        Set<String> allowedSet = new HashSet<>(parentAllowedFormIds);

        List<Integer> allChildTeamIds = descendantTeamIds(teamId);
        if (allChildTeamIds.isEmpty()) return;

        for (Integer childTeamId : allChildTeamIds) {
            List<String> childForms =
                    teamFormRepository.findByTeamId(childTeamId)
                            .stream()
                            .map(teamForm -> teamForm.getId().getFormId())
                            .toList();

            List<String> targetRemovalFormIds = childForms.stream()
                    .filter(id -> !allowedSet.contains(id))
                    .toList();

            if (!targetRemovalFormIds.isEmpty()) {
                teamFormRepository.deleteByTeamIdAndFormIdIn(childTeamId, targetRemovalFormIds);
            }
        }
    }

    /**
     * Set target team's leader to null if role is not allowed, determine by team's depth/level
     * depth 1: allow supervisor only
     * depth 2 and below: allow team lead only
     */
    @Transactional
    public void verifyAndUpdateLeader(Integer teamId, Short roleId) {
        int teamDepth = getDepth(teamId);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team " + teamId + " not found"));

        if (roleId == 1) {
            // Supervisor role only allowed on team with depth 1
            if (teamDepth != 1) {
                team.setLeader(null);
            }
        } else if (roleId == 2) {
            // Worker role is not allowed to be leader
            team.setLeader(null);
        } else if (roleId == 3) {
            // Team lead role only allowed on depth other than 1
            if (teamDepth == 1) {
             team.setLeader(null);
            }
        } else if (roleId == 4) {
            // Manager role only allowed on team with depth 1
            if (teamDepth != 1) {
                team.setLeader(null);
            }
        }

        teamRepository.save(team);
    }

    /**
     * ids of *all* descendants (NOT including the team itself)
     */
    private List<Integer> descendantTeamIds(Integer teamId) {
        List<Integer> all = teamRepository.findSelfAndDescendantIds(teamId);
        all.remove(teamId);
        return all;
    }

    private TeamDTO mapWithChildren(Team entity, int level) {
        TeamDTO dto = modelMapper.map(entity, TeamDTO.class);
        dto.setLevel(level);

        dto.setAssociatedFormCount(teamFormRepository.findByTeamId(entity.getId()).size());
        dto.setMemberCount(teamUserRepository.findByIdTeamId(entity.getId()).size());

        if (entity.getParent() != null){
            dto.setParentId(entity.getParent().getId());
        }

        if (entity.getChildren() != null && !entity.getChildren().isEmpty()) {
            dto.setChildren(entity.getChildren().stream()
                    .sorted(Comparator.comparing(Team::getId))
                    .map(c-> mapWithChildren(c, level + 1))
                    .toList());
        }

        return dto;
    }
}

