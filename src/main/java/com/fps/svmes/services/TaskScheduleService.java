package com.fps.svmes.services;

import com.fps.svmes.models.sql.taskSchedule.Dispatch;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Map;

public interface TaskScheduleService {

    void scheduleDispatchTask(Dispatch dispatch, Runnable task);
    boolean cancelDispatch(Long dispatchId);
    boolean isScheduled(Long dispatchId);
    OffsetDateTime getNextExecutionTime(Long dispatchId);
    public Map<Long, OffsetDateTime> getAllScheduledTasks();
    public void scheduleOneTimeTask(OffsetDateTime executionTime, Runnable task);
}