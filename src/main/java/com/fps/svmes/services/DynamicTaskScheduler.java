package com.fps.svmes.services;

public interface DynamicTaskScheduler {

    public void scheduleDispatchTask(Long dispatchId, Runnable task, String cronExpression);
    public void cancelScheduledTask(Long dispatchId);

}