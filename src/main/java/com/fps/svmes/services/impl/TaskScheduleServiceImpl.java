package com.fps.svmes.services.impl;

import com.fps.svmes.models.sql.taskSchedule.TaskType;

import com.fps.svmes.models.sql.taskSchedule.Dispatch;
import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchRepository;
import com.fps.svmes.services.TaskScheduleService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class TaskScheduleServiceImpl implements TaskScheduleService {

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private DispatchRepository dispatchRepo;

    // Map to track scheduled tasks by dispatch ID
    private final Map<Long, Map<TaskType, ScheduledFuture<?>>> tasks = new ConcurrentHashMap<>();
    // Centralized state management
    private static final Logger logger = LoggerFactory.getLogger(TaskScheduleServiceImpl.class);

    /**
     * Schedule a dispatch task if it's active and within the defined period.
     *
     * @param dispatch the dispatch entity containing scheduling details.
     * @param task     the task to be executed.
     */
    public void scheduleDispatch(Dispatch dispatch, Runnable task) {

        // 1. schedule a cron periodic task for this dispatch
        CronTrigger ct = new CronTrigger(dispatch.getCronExpression());
        ScheduledFuture<?> cronFuture = taskScheduler.schedule(task, ct);

        if (cronFuture != null) {
            // track this cron runnable in tasks map
            addTask(dispatch.getId(), TaskType.CRON, cronFuture);
        } else {
            throw new IllegalStateException("Failed to schedule cron periodic task for Dispatch ID: " + dispatch.getId());
        }

        Runnable cancelRunnable = ()->{
            removeTask(dispatch.getId(), TaskType.CRON);
            removeTask(dispatch.getId(), TaskType.CANCEL);

            // set the dispatch is active to false since all tasks is canceled
            Dispatch d = dispatchRepo.findById(dispatch.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));
            d.setIsActive(false);
            d.setUpdatedAt(OffsetDateTime.now());
            dispatchRepo.save(d);
        };

        // 2. schedule a cancel task at the end time with 30 seconds delay for this dispatch
        ScheduledFuture<?> cancelFuture = taskScheduler.schedule(cancelRunnable, dispatch.getEndTime().plusSeconds(30).toInstant());

        // track this cancel runnable in tasks map
        addTask(dispatch.getId(), TaskType.CANCEL, cancelFuture);
    }

    /**
     * Schedule a dispatch task if it's defined period is in the future.
     *
     * @param dispatch the dispatch entity containing scheduling details.
     * @param task     the task to be executed.
     */
    @Override
    public void scheduleFutureDispatch(Dispatch dispatch, Runnable task) {
        if (dispatch.getStartTime().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Dispatch start time must be in the future");
        }

        if (!isScheduled(dispatch.getId())) {
            Runnable scheduleCronTaskAndCancelTask = ()->{
                CronTrigger ct = new CronTrigger(dispatch.getCronExpression());
                ScheduledFuture<?> cronFuture = taskScheduler.schedule(task, ct);

                if (cronFuture != null) {
                    addTask(dispatch.getId(), TaskType.CRON, cronFuture);
                } else {
                    throw new IllegalStateException("Failed to schedule cron periodic task for Dispatch ID: " + dispatch.getId());
                }


                ScheduledFuture<?> cancelFuture = taskScheduler.schedule(()->{
                    removeTask(dispatch.getId(), TaskType.CRON);
                    removeTask(dispatch.getId(), TaskType.CANCEL);
                    // set the dispatch is active to false since all tasks is canceled
                    Dispatch d = dispatchRepo.findById(dispatch.getId())
                            .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));
                    d.setIsActive(false);
                    d.setUpdatedAt(OffsetDateTime.now());
                    dispatchRepo.save(d);
                }, dispatch.getEndTime().plusSeconds(30).toInstant());

                addTask(dispatch.getId(), TaskType.CANCEL, cancelFuture);
                removeTask(dispatch.getId(), TaskType.FUTURE);
            };

            ScheduledFuture<?> futureTask = taskScheduler.schedule(scheduleCronTaskAndCancelTask, dispatch.getStartTime().toInstant());
            addTask(dispatch.getId(), TaskType.FUTURE, futureTask);

        } else {
            logger.warn("Dispatch ID {} is already scheduled for future execution.", dispatch.getId());
        }
    }


    /**
     * Check if any task is currently scheduled for a given dispatch ID.
     *
     * @param dispatchId the ID of the dispatch to check.
     * @return true if any type of task is scheduled, false otherwise.
     */
    public boolean isScheduled(Long dispatchId) {
        Map<TaskType, ScheduledFuture<?>> typeMap = tasks.get(dispatchId);
        return typeMap != null && !typeMap.isEmpty();
    }


    /**
     * Retrieve the next scheduled execution time for a given dispatch ID and given type if exist and not cancelled.
     *
     * @param dispatchId the ID of the dispatch to check.
     * @param type the Type of the task to check
     * @return the next execution time as a OffsetDateTime, or null if not scheduled.
     */

    public OffsetDateTime getNextExecutionTime(Long dispatchId, TaskType type) {
        Map<TaskType, ScheduledFuture<?>> typeMap = tasks.get(dispatchId);
        if (typeMap != null) {
            ScheduledFuture<?> future = tasks.get(dispatchId).get(type);
            if (future != null && !future.isCancelled()) {
                long delay = future.getDelay(TimeUnit.MILLISECONDS);
                if (delay > 0) {
                    return OffsetDateTime.now(ZoneOffset.UTC).plusNanos(delay * 1_000_000);
                }
            }
        }

        return null; // Return null if no next execution time is available
    }

    /**
     * Retrieve all scheduled dispatch's tasks with their next execution times.
     *
     * @return A map where the key is the dispatch ID and the map of task type and their next execution time.
     */
    public Map<Long, Map<TaskType, OffsetDateTime>> getAllScheduledTasks() {
        Map<Long, Map<TaskType, OffsetDateTime>> result = new ConcurrentHashMap<>();

        tasks.forEach((dispatchId, typeMap) -> {
            Map<TaskType, OffsetDateTime> taskExecutionTimes = new ConcurrentHashMap<>();

            typeMap.forEach((taskType, future) -> {
                if (future != null && !future.isCancelled()) {
                    long delay = future.getDelay(TimeUnit.MILLISECONDS);
                    OffsetDateTime nextExecutionTime = delay > 0
                            ? OffsetDateTime.now(ZoneOffset.UTC).plusNanos(delay * 1_000_000)
                            : null;
                    taskExecutionTimes.put(taskType, nextExecutionTime);
                }
            });

            if (!taskExecutionTimes.isEmpty()) {
                result.put(dispatchId, taskExecutionTimes);
            }
        });

        return result;
    }

    public void addTask(long dispatchId, TaskType type, ScheduledFuture<?> future) {
        // Ensure the inner map exists
        tasks.computeIfAbsent(dispatchId, id -> new ConcurrentHashMap<>())
                .put(type, future);

        ScheduledFuture<?> futureAdded = tasks.get(dispatchId).get(type);
        if (futureAdded != null) {
            logger.info("Scheduled task of type {} for dispatch id {}", type , dispatchId);
        }


    }

    public ScheduledFuture<?> getTask(long dispatchId, TaskType type) {
        Map<TaskType, ScheduledFuture<?>> typeMap = tasks.get(dispatchId);
        return (typeMap != null) ? typeMap.get(type) : null;
    }

    public boolean removeTask(long dispatchId, TaskType type) {
        Map<TaskType, ScheduledFuture<?>> typeMap = tasks.get(dispatchId);
        if (typeMap != null) {
            ScheduledFuture<?> future = typeMap.remove(type);
            if (future != null) {
                future.cancel(true);
                if (future.isCancelled()) {
                    logger.info("Removed task of type {} for dispatch id {}", type , dispatchId);
                }
                return future.isCancelled();
            }
        }
        return false;
    }

    public boolean removeAllTasks(long dispatchId) {
        Map<TaskType, ScheduledFuture<?>> typeMap = tasks.remove(dispatchId);
        if (typeMap != null) {
            typeMap.values().forEach(future -> future.cancel(true));
            logger.info("Removed all scheduled tasks for dispatch id {}", dispatchId);
            return true;
        }
        return false;
    }

    public void cleanup() {
        // Iterate and remove completed tasks
        tasks.values().forEach(typeMap ->
                typeMap.entrySet().removeIf(entry -> entry.getValue().isDone() || entry.getValue().isCancelled())
        );
    }
}