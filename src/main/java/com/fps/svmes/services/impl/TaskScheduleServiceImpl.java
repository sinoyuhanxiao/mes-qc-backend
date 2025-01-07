package com.fps.svmes.services.impl;

import com.fps.svmes.models.sql.task_schedule.Dispatch;
import com.fps.svmes.services.TaskScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class TaskScheduleServiceImpl implements TaskScheduleService {

    @Autowired
    private TaskScheduler taskScheduler;

    // Map to track scheduled tasks by dispatch ID
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(TaskScheduleServiceImpl.class);


    /**
     * Schedule a dispatch task if it's active and within the defined period.
     *
     * @param dispatch the dispatch entity containing scheduling details.
     * @param task     the task to be executed.
     */
    public void scheduleDispatchTask(Dispatch dispatch, Runnable task) {
        cancelDispatch(dispatch.getId()); // Cancel existing task if any
        ScheduledFuture<?> future = taskScheduler.schedule(task, new CronTrigger(dispatch.getCronExpression()));

        if (future != null) {
            scheduledTasks.put(dispatch.getId(), future);
            logger.info("Scheduled a dispatching task for dispatch id {}", dispatch.getId());
        } else {
            throw new IllegalStateException("Failed to schedule task for Dispatch ID: " + dispatch.getId());
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
     * @return the next execution time as a Timestamp, or null if not scheduled.
     */
    public Timestamp getNextExecutionTime(Long dispatchId) {
        ScheduledFuture<?> future = scheduledTasks.get(dispatchId);
        if (future != null && !future.isCancelled()) {
            long delay = future.getDelay(java.util.concurrent.TimeUnit.MILLISECONDS);
            return delay > 0 ? new Timestamp(System.currentTimeMillis() + delay) : null;
        }
        return null;
    }

    /**
     * Retrieve all scheduled tasks with their next execution times.
     *
     * @return A map where the key is the dispatch ID and the value is the next execution time.
     */
    public Map<Long, Timestamp> getAllScheduledTasks() {
        Map<Long, Timestamp> result = new HashMap<>();
        scheduledTasks.forEach((id, future) -> {
            if (future != null && !future.isCancelled()) {
                long delay = future.getDelay(java.util.concurrent.TimeUnit.MILLISECONDS);
                result.put(id, delay > 0 ? new Timestamp(System.currentTimeMillis() + delay) : null);
            }
        });
        return result;
    }

    /**
     * Update an existing scheduled dispatch task if necessary.
     *
     * @param dispatch the updated dispatch entity.
     * @param task     the new task to execute.
     */
    public void updateDispatchTask(Dispatch dispatch, Runnable task) {
        if (isScheduled(dispatch.getId())) {
            cancelDispatch(dispatch.getId());
        }
        scheduleDispatchTask(dispatch, task);
    }

    /**
     * Log scheduled task details for monitoring.
     *
     * @param dispatchId the ID of the dispatch.
     */
    public void logScheduledTaskDetails(Long dispatchId) {
        ScheduledFuture<?> future = scheduledTasks.get(dispatchId);
        if (future != null) {
            System.out.println("Task ID: " + dispatchId + ", Next Execution Time: " + getNextExecutionTime(dispatchId));
        } else {
            System.out.println("Task ID: " + dispatchId + " is not currently scheduled.");
        }
    }
}