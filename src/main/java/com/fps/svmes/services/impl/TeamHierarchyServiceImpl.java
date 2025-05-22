package com.fps.svmes.services.impl;

import com.fps.svmes.repositories.jpaRepo.user.TeamFormRepository;
import com.fps.svmes.repositories.jpaRepo.user.TeamRepository;
import com.fps.svmes.repositories.jpaRepo.user.TeamUserRepository;
import com.fps.svmes.services.TeamHierarchyService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamHierarchyServiceImpl implements TeamHierarchyService {

    private final TeamRepository teamRepo;
    private final TeamUserRepository teamUserRepo;
    private final TeamFormRepository teamFormRepo;

    @Override
    @Transactional
    public void syncUsers(Integer teamId, List<Integer> parentAllowedUserIds) {
        Set<Integer> allowedSet = new HashSet<>(parentAllowedUserIds);

        List<Integer> childTeamIds = descendantTeamIds(teamId);
        if (childTeamIds.isEmpty()) return;

        for (Integer childTeamId : childTeamIds) {
            List<Integer> childUsers = teamUserRepo.findByIdTeamId(childTeamId)
                    .stream()
                    .map(teamUser -> teamUser.getUser().getId())
                    .toList();

            List<Integer> targetRemovalUserIds = childUsers.stream()
                    .filter(id -> !allowedSet.contains(id))
                    .toList();

            if (!targetRemovalUserIds.isEmpty()) {
                teamUserRepo.deleteByTeamIdAndUserIdIn(childTeamId, targetRemovalUserIds);
            }
        }
    }

    @Override
    @Transactional
    public void syncForms(Integer teamId, List<String> parentAllowedFormIds) {
        Set<String> allowedSet = new HashSet<>(parentAllowedFormIds);

        List<Integer> allChildTeamIds = descendantTeamIds(teamId);
        if (allChildTeamIds.isEmpty()) return;

        for (Integer childTeamId : allChildTeamIds) {
            List<String> childForms =
                    teamFormRepo.findByTeamId(childTeamId)
                            .stream()
                            .map(teamForm -> teamForm.getId().getFormId())
                            .toList();

            List<String> targetRemovalFormIds = childForms.stream()
                    .filter(id -> !allowedSet.contains(id))
                    .toList();

            if (!targetRemovalFormIds.isEmpty()) {
                teamFormRepo.deleteByTeamIdAndFormIdIn(childTeamId, targetRemovalFormIds);
            }
        }
    }

    @Override
    public List<Integer> descendantTeamIds(Integer teamId) {
        List<Integer> all = teamRepo.findSelfAndDescendantIds(teamId);
        all.remove(teamId);
        return all;
    }
}

