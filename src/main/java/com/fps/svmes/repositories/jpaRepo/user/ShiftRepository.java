package com.fps.svmes.repositories.jpaRepo.user;

import com.fps.svmes.models.sql.user.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Integer> {
}