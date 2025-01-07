package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.dispatch.DispatchedTaskDTO;

import java.util.List;

public interface DispatchedTaskService {
    List<DispatchedTaskDTO> getCurrentTasks(Long userId);
    List<DispatchedTaskDTO> getFutureTasks(Long userId);
    List<DispatchedTaskDTO> getHistoricalTasks(Long userId);
    List<DispatchedTaskDTO> getOverdueTasks(Long userId);
    void insertDispatchedTasks(DispatchedTaskDTO dispatchedTaskDTO, List<Integer> userIds);
    void updateDispatchedTask(Long id, DispatchedTaskDTO dispatchedTaskDTO);
    DispatchedTaskDTO getDispatchedTaskById(Long id);
    void deleteDispatchedTask(Long id);
}
