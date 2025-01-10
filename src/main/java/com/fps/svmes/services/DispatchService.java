package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
import com.fps.svmes.dto.dtos.dispatch.DispatchedTaskDTO;
import com.fps.svmes.dto.requests.DispatchRequest;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Service interface for handling dispatch logic.
 */
public interface DispatchService {
    void executeDispatch(Long dispatchId);
    void scheduleDispatches();
    DispatchDTO createDispatch(@Valid DispatchRequest request);
    DispatchDTO createManualDispatch(DispatchRequest request);
    DispatchDTO updateDispatch(Long id, @Valid DispatchRequest request);
    DispatchDTO getDispatch(Long id);
    List<DispatchDTO> getAllDispatches();
    List<DispatchedTaskDTO> getAllDispatchedTasks();
    void deleteDispatch(Long id);
    void scheduleDispatch(Long dispatchId, Runnable task);
    void cancelDispatchTask(Long dispatchId);
}
