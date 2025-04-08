package com.fps.svmes.repositories.jpaRepo.user;

import com.fps.svmes.models.sql.user.ShiftForm;
import com.fps.svmes.models.sql.user.ShiftFormId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftFormRepository extends JpaRepository<ShiftForm, ShiftFormId> {
    List<ShiftForm> findByShiftId(Integer shiftId);
    boolean existsById(ShiftFormId shiftFormId);
    void deleteByShiftId(Integer shiftId);
    void deleteById_FormId(String formId);

    @Modifying
    @Query("DELETE FROM ShiftForm sf WHERE sf.id.formId IN :formIds")
    void deleteAllByFormIds(@Param("formIds") List<String> formIds);
}
