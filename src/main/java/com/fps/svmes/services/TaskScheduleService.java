package com.fps.svmes.services;

import com.fps.svmes.models.sql.task_schedule.Dispatch;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Map;

public interface TaskScheduleService {

    void scheduleDispatchTask(Dispatch dispatch, Runnable task);
    boolean cancelDispatch(Long dispatchId);
    boolean isScheduled(Long dispatchId);
    Timestamp getNextExecutionTime(Long dispatchId);
    void logScheduledTaskDetails(Long dispatchId);
    public Map<Long, Timestamp> getAllScheduledTasks();
    public void scheduleOneTimeTask(OffsetDateTime executionTime, Runnable task);
}