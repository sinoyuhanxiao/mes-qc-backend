package com.fps.svmes.repositories.jpaRepo.dispatch;

import com.fps.svmes.models.sql.task_schedule.DispatchedTest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispatchedTestRepository extends JpaRepository<DispatchedTest, Long> {
}
