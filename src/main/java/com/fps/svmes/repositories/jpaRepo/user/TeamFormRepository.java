package com.fps.svmes.repositories.jpaRepo.user;

import com.fps.svmes.models.sql.user.TeamForm;
import com.fps.svmes.models.sql.user.TeamFormId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamFormRepository extends JpaRepository<TeamForm, TeamFormId> {
    List<TeamForm> findByTeamId(Integer teamId);
    boolean existsById(TeamFormId teamFormId);
    void deleteByTeamId(Integer teamId);
    void deleteById_FormId(String formId);

    @Modifying
    @Query("DELETE FROM TeamForm sf WHERE sf.id.formId IN :formIds")
    void deleteAllByFormIds(@Param("formIds") List<String> formIds);
}
