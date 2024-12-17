package com.fps.svmes.services;

import com.fps.svmes.dto.requests.DispatchRequest;
import com.fps.svmes.models.sql.task_schedule.Dispatch;
import jakarta.validation.Valid;

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
    Dispatch createDispatch(@Valid DispatchRequest request);
    Dispatch updateDispatch(Long id, @Valid DispatchRequest request);
    Dispatch getDispatch(Long id);
    List<Dispatch> getAllDispatches();
    void deleteDispatch(Long id);
    boolean manualDispatch(Long id);
}
