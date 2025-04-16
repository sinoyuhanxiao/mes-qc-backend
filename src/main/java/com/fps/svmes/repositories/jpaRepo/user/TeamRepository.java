package com.fps.svmes.repositories.jpaRepo.user;

import com.fps.svmes.models.sql.user.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Integer> {
}