package com.fps.svmes.services;

import com.fps.svmes.models.sql.task_schedule.Dispatch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for handling dispatch logic.
 */
public interface DispatchService {
    void executeDispatch(Long dispatchId);
    void scheduleDispatches();
    boolean shouldDispatch(Dispatch dispatch, LocalDateTime now);
    Dispatch createDispatch(Dispatch dispatch);
    Optional<Dispatch> getDispatchById(Long id);
    List<Dispatch> getAllDispatches();
    Optional<Dispatch> updateDispatch(Long id, Dispatch updatedDispatch);
    boolean deleteDispatch(Long id);
    boolean manualDispatch(Long id);
}
