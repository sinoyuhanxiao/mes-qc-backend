package com.fps.svmes.services;

import com.fps.svmes.models.sql.Dispatch;

import java.time.LocalDateTime;

/**
 * Service interface for handling dispatch logic.
 */
public interface DispatchService {
    void executeDispatch(Long dispatchId);
    void scheduleDispatches();
    boolean shouldDispatch(Dispatch dispatch, LocalDateTime now);
}
