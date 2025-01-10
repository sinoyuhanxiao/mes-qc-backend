package com.fps.svmes.services.impl;

import com.fps.svmes.models.sql.taskSchedule.Dispatch;
import com.fps.svmes.models.sql.taskSchedule.TaskState;
import com.fps.svmes.services.TaskScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public enum TaskType {
    CRON(1),
    FUTURE(2),
    CANCEL(3);

    private final int type;

    TaskType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}


@Service
public class TaskScheduleServiceImpl implements TaskScheduleService {

    @Autowired
    private TaskScheduler taskScheduler;

    // Map to track scheduled tasks by dispatch ID
    private final Map<Long, Map<TaskType, ScheduledFuture<?>>> tasks = new ConcurrentHashMap<>();
    // Centralized state management
    private static final Logger logger = LoggerFactory.getLogger(TaskScheduleServiceImpl.class);


    @Scheduled(fixedRate = 600000) // Runs every 10 minutes
    public void cleanupTaskStates() {
        OffsetDateTime now = OffsetDateTime.now();
        taskStateMap.forEach((dispatchId, state) -> {
            if (!state.isScheduled() || now.isAfter(getNextExecutionTime(dispatchId))) {
                taskStateMap.remove(dispatchId);
                logger.info("Cleaned up task state for Dispatch ID {}", dispatchId);
            }
        });
    }

    /**
     * Schedule a dispatch task if it's active and within the defined period.
     *
     * @param dispatch the dispatch entity containing scheduling details.
     * @param task     the task to be executed.
     */
    public void scheduleDispatchCronTask(Dispatch dispatch, Runnable task) {

        CronTrigger ct = new CronTrigger(dispatch.getCronExpression());
        ScheduledFuture<?> future = taskScheduler.schedule(task, ct);

        if (future != null) {
            addTask(dispatch.getId(), TaskType.CRON, future);
        } else {
            throw new IllegalStateException("Failed to schedule cron periodic task for Dispatch ID: " + dispatch.getId());
        }

        taskScheduler.schedule(()->{
            cancelDispatch(dispatch.getId());
        }, dispatch.getEndTime().plusSeconds(30).toInstant());

    }


    /**
     * Cancel a scheduled task by its dispatch ID.
     *
     * @param dispatchId the ID of the dispatch to cancel.
     */
    public boolean cancelDispatch(Long dispatchId) {
        ScheduledFuture<?> future = tasks.get(dispatchId).get(TaskType.CRON);
        if (future != null) {
            future.cancel(true);
            if (future.isCancelled()) {
                logger.info("Canceled scheduled tasks for dispatch with id {}", dispatchId);
                removeTask(dispatchId, TaskType.CRON);
                removeTask(dispatchId, TaskType.CANCEL);
            }
        }
        return future != null;
    }


    /**
     * Check if a task is currently scheduled for a given dispatch ID.
     *
     * @param dispatchId the ID of the dispatch to check.
     * @return true if the task is scheduled, false otherwise.
     */
    public boolean isScheduled(Long dispatchId) {
        Map<TaskType, ScheduledFuture<?>> typeMap = tasks.get(dispatchId);
        return typeMap != null && !typeMap.isEmpty();
    }


    /**
     * Retrieve the next scheduled execution time for a given dispatch ID.
     *
     * @param dispatchId the ID of the dispatch to check.
     * @return the next execution time as a OffsetDateTime, or null if not scheduled.
     */

    public OffsetDateTime getNextExecutionTime(Long dispatchId) {
        ScheduledFuture<?> future = scheduledTasks.get(dispatchId);
        if (future != null && !future.isCancelled()) {
            long delay = future.getDelay(TimeUnit.MILLISECONDS);
            if (delay > 0) {
                return OffsetDateTime.now(ZoneOffset.UTC).plusNanos(delay * 1_000_000);
            }
        }
        return null; // Return null if no next execution time is available
    }

    /**
     * Retrieve all scheduled tasks with their next execution times.
     *
     * @return A map where the key is the dispatch ID and the value is the next execution time.
     */
    public Map<Long, OffsetDateTime> getAllScheduledTasks() {
        Map<Long, OffsetDateTime> result = new ConcurrentHashMap<>();
        scheduledTasks.forEach((id, future) -> {
            if (future != null && !future.isCancelled()) {
                long delay = future.getDelay(TimeUnit.MILLISECONDS);
                result.put(id, delay > 0 ? OffsetDateTime.now(ZoneOffset.UTC).plusNanos(delay * 1_000_000) : null);
            }
        });
        return result;
    }

    @Override
    public void scheduleOneTimeTask(OffsetDateTime executionTime, Runnable task) {
        OffsetDateTime now = OffsetDateTime.now();

        synchronized (taskStateMap) { // Use taskStateMap or another object for synchronization
            if (executionTime.isAfter(now)) {
                ScheduledFuture<?> future = taskScheduler.schedule(task, triggerContext -> Date.from(executionTime.toInstant()).toInstant());
                if (future != null) {
                    logger.info("One-time task scheduled for execution at {}", executionTime);
                } else {
                    logger.warn("Failed to schedule one-time task for execution at {}", executionTime);
                }
            } else {
                logger.warn("Attempted to schedule a one-time task for a past time: {}", executionTime);
            }
        }
    }

    @Override
    public void scheduleDispatchStartingAt(Dispatch dispatch, Runnable task) {
        if (dispatch.getStartTime().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Dispatch start time must be in the future");
        }

        logger.info("scheduling scheduleDispatchTask for dispatch with ID {}", dispatch.getId());
        if (!isScheduled(dispatch.getId())) {
            taskScheduler.schedule(()=>{

            } ,dispatch.getStartTime());

        } else {
            logger.warn("Dispatch ID {} is already scheduled for future execution.", dispatch.getId());
        }
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
                future.cancel(false);
                return true;
            }
        }
        return false;
    }

    public boolean removeAllTasks(long dispatchId) {
        Map<TaskType, ScheduledFuture<?>> typeMap = tasks.remove(dispatchId);
        if (typeMap != null) {
            typeMap.values().forEach(future -> future.cancel(false));
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