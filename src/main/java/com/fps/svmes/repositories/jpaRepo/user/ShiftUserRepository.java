package com.fps.svmes.repositories.jpaRepo.user;

import com.fps.svmes.dto.dtos.user.ShiftForUserTableDTO;
import com.fps.svmes.models.sql.user.ShiftUser;
import com.fps.svmes.models.sql.user.ShiftUserId;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftUserRepository extends JpaRepository<ShiftUser, Integer> {

    List<ShiftUser> findByIdShiftId(Integer shiftId);

    List<ShiftUser> findByIdUserId(Integer userId);

    void deleteByIdUserId(Integer userId);

    void deleteByIdShiftId(Integer shiftId);

    void deleteById(ShiftUserId shiftUserId);

    @Query("SELECT new com.fps.svmes.dto.dtos.user.ShiftForUserTableDTO(s.id, s.name, s.leader.name) " +
            "FROM ShiftUser su " +
            "JOIN su.shift s " +
            "WHERE su.id.userId = :userId")
    List<ShiftForUserTableDTO> findShiftsByUserId(@Param("userId") Integer userId);
}