package com.fps.svmes.services;

import java.util.List;

public interface TeamHierarchyService {

    /**
     * ids of *all* descendants (NOT including the team itself)
     */
    List<Integer> descendantTeamIds(Integer teamId);

    /**
     * remove users from subtree if they are no longer in *parentAllowed*
     */
    void syncUsers(Integer teamId, List<Integer> parentAllowedUserIds);

    /**
     * remove forms from subtree if they are no longer in *parentAllowed*
     */
    void syncForms(Integer teamId, List<String> parentAllowedFormIds);
}
