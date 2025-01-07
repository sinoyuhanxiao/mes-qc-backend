package com.fps.svmes.repositories.jpaRepo.dispatch;

import com.fps.svmes.models.sql.taskSchedule.DispatchedTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispatchedTaskRepository extends JpaRepository<DispatchedTask, Long> {

    // Find tasks for the current day
    List<DispatchedTask> findByUserIdAndDueDateBetweenAndStatus(
            Long userId, java.time.OffsetDateTime startOfDay, java.time.OffsetDateTime endOfDay, Integer status);

    // Find tasks with due dates after today (future tasks)
    List<DispatchedTask> findByUserIdAndDueDateAfterAndStatus(Long userId, java.time.OffsetDateTime dueDate, Integer status);

    // Find tasks with due dates before today (historical tasks)
    List<DispatchedTask> findByUserIdAndDueDateBeforeAndStatus(Long userId, java.time.OffsetDateTime dueDate, Integer status);

    // Find overdue tasks
    List<DispatchedTask> findByUserIdAndIsOverdueAndStatus(Long userId, Boolean isOverdue, Integer status);

}
