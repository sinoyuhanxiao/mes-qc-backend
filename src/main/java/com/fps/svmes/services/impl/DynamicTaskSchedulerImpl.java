package com.fps.svmes.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class DynamicTaskSchedulerImpl {
    @Autowired
    private TaskScheduler taskScheduler;

    // Map to track scheduled tasks by dispatch ID
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public void scheduleDispatchTask(Long dispatchId, Runnable task, String cronExpression) {
        if (scheduledTasks.containsKey(dispatchId)) {
            cancelScheduledTask(dispatchId);
        }

        // Schedule the task with the given cron expression
        ScheduledFuture<?> scheduledFuture = ((ThreadPoolTaskScheduler) taskScheduler)
                .schedule(task, new CronTrigger(cronExpression));
        scheduledTasks.put(dispatchId, scheduledFuture);
    }

    public void cancelScheduledTask(Long dispatchId) {
        ScheduledFuture<?> scheduledFuture = scheduledTasks.remove(dispatchId);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }
}