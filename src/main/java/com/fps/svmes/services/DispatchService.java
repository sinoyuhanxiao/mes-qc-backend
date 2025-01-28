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
    DispatchDTO updateDispatch(Long id, @Valid DispatchRequest request);
    DispatchDTO getDispatch(Long id);
    List<DispatchDTO> getAllDispatches();
    List<DispatchedTaskDTO> getAllDispatchedTasks();
    void pauseDispatch(Long dispatchId, Integer userId);
    void resumeDispatch(Long dispatchId, Integer userId);
    void deleteDispatch(Long id);
    void initializeDispatch(Long dispatchId, Runnable task);
    void cancelDispatchTask(Long dispatchId);
}
