package com.fps.svmes.repositories.jpaRepo.user;

import com.fps.svmes.models.sql.user.ShiftUser;
import com.fps.svmes.models.sql.user.ShiftUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftUserRepository extends JpaRepository<ShiftUser, Long> {

    List<ShiftUser> findByIdShiftId(Long shiftId);

    List<ShiftUser> findByIdUserId(Long userId);

    void deleteByIdUserId(Long userId);

    void deleteById(ShiftUserId shiftUserId);

}