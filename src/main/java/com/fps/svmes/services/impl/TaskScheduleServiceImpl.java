package com.fps.svmes.services.impl;

import com.fps.svmes.models.sql.taskSchedule.Dispatch;
import com.fps.svmes.services.TaskScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class TaskScheduleServiceImpl implements TaskScheduleService {

    @Autowired
    private TaskScheduler taskScheduler;

    // Map to track scheduled tasks by dispatch ID
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> schedulingInProgress = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(TaskScheduleServiceImpl.class);


    /**
     * Schedule a dispatch task if it's active and within the defined period.
     *
     * @param dispatch the dispatch entity containing scheduling details.
     * @param task     the task to be executed.
     */
    public void scheduleDispatchTask(Dispatch dispatch, Runnable task) {
        if (schedulingInProgress.putIfAbsent(dispatch.getId(), true) != null) {
            logger.warn("Dispatch ID {} is already being scheduled. Skipping redundant request.", dispatch.getId());
            return;
        }

        synchronized (scheduledTasks) { // Lock for task scheduling map to avoid race conditions
            try {
                cancelDispatch(dispatch.getId()); // Cancel existing task if any
                ScheduledFuture<?> future = taskScheduler.schedule(task, new CronTrigger(dispatch.getCronExpression()));

                if (future != null) {
                    scheduledTasks.put(dispatch.getId(), future);
                    logger.info("Scheduled a dispatching task for dispatch id {}", dispatch.getId());
//                    logger.info("Scheduled a dispatching task for dispatch id {} to assign {} forms to {} users", dispatch.getId(), dispatch.getDispatchForms().size(), dispatch.getDispatchUsers().size());
                } else {
                    throw new IllegalStateException("Failed to schedule task for Dispatch ID: " + dispatch.getId());
                }
            } finally {
                schedulingInProgress.remove(dispatch.getId());
            }

        }
    }

    /**
     * Cancel a scheduled task by its dispatch ID.
     *
     * @param dispatchId the ID of the dispatch to cancel.
     */
    public boolean cancelDispatch(Long dispatchId) {
        ScheduledFuture<?> future = scheduledTasks.get(dispatchId);
        if (future != null) {
            future.cancel(false);
            scheduledTasks.remove(dispatchId);
            logger.info("Canceled dispatching task for dispatch id {}", dispatchId);
            return true;
        }
        return false;
    }


    /**
     * Check if a task is currently scheduled for a given dispatch ID.
     *
     * @param dispatchId the ID of the dispatch to check.
     * @return true if the task is scheduled, false otherwise.
     */
    public boolean isScheduled(Long dispatchId) {
        return scheduledTasks.containsKey(dispatchId);
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
            long delay = future.getDelay(java.util.concurrent.TimeUnit.MILLISECONDS);
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
                long delay = future.getDelay(java.util.concurrent.TimeUnit.MILLISECONDS);
                result.put(id, delay > 0 ? OffsetDateTime.now(ZoneOffset.UTC).plusNanos(delay * 1_000_000) : null);
            }
        });
        return result;
    }

    public void scheduleOneTimeTask(OffsetDateTime executionTime, Runnable task) {
        OffsetDateTime now = OffsetDateTime.now();

        if (executionTime.isAfter(now)) {
            long delay = executionTime.toInstant().toEpochMilli() - System.currentTimeMillis();

            synchronized (schedulingInProgress) {
                if (schedulingInProgress.putIfAbsent((long) task.hashCode(), true) != null) {
                    logger.warn("One-time task for execution at {} is already being scheduled. Skipping.", executionTime);
                    return;
                }

                try {
                    taskScheduler.schedule(task, triggerContext -> Date.from(executionTime.toInstant()).toInstant());
                    logger.info("One-time task scheduled for execution at {}", executionTime);
                } finally {
                    schedulingInProgress.remove((long) task.hashCode());
                }
            }
        } else {
            logger.warn("Attempted to schedule a one-time task for a past time: {}", executionTime);
        }
    }
}