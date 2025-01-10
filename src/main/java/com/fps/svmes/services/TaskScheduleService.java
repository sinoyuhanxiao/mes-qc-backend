package com.fps.svmes.services;

import com.fps.svmes.models.sql.taskSchedule.Dispatch;
import com.fps.svmes.models.sql.taskSchedule.TaskState;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Map;

public interface TaskScheduleService {

    void scheduleDispatchCronTask(Dispatch dispatch, Runnable task);
    void scheduleDispatchStartingAt(Dispatch dispatch, Runnable task);
    void scheduleOneTimeTask(OffsetDateTime executionTime, Runnable task);
    boolean cancelDispatch(Long dispatchId);
    boolean isScheduled(Long dispatchId);
    OffsetDateTime getNextExecutionTime(Long dispatchId);
    Map<Long, OffsetDateTime> getAllScheduledTasks();
    TaskState getTaskState(Long dispatchId);
}