package com.fps.svmes.repositories.jpaRepo.dispatch;

import com.fps.svmes.models.sql.task_schedule.DispatchedTaskTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispatchedTaskTestRepository extends JpaRepository<DispatchedTaskTest, Long> {

    // Find tasks for the current day
    List<DispatchedTaskTest> findByUserIdAndDueDateBetweenAndStatus(
            Long userId, java.time.OffsetDateTime startOfDay, java.time.OffsetDateTime endOfDay, Integer status);

    // Find tasks with due dates after today (future tasks)
    List<DispatchedTaskTest> findByUserIdAndDueDateAfterAndStatus(Long userId, java.time.OffsetDateTime dueDate, Integer status);

    // Find tasks with due dates before today (historical tasks)
    List<DispatchedTaskTest> findByUserIdAndDueDateBeforeAndStatus(Long userId, java.time.OffsetDateTime dueDate, Integer status);

    // Find overdue tasks
    List<DispatchedTaskTest> findByUserIdAndIsOverdueAndStatus(Long userId, Boolean isOverdue, Integer status);

}
