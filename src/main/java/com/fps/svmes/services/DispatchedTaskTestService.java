package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.dispatch.DispatchedTaskTestDTO;

import java.util.List;

public interface DispatchedTaskTestService {
    List<DispatchedTaskTestDTO> getCurrentTasks(Long userId);
    List<DispatchedTaskTestDTO> getFutureTasks(Long userId);
    List<DispatchedTaskTestDTO> getHistoricalTasks(Long userId);
    List<DispatchedTaskTestDTO> getOverdueTasks(Long userId);
    void insertDispatchedTasks(DispatchedTaskTestDTO dispatchedTaskDTO, List<Integer> userIds);
    void updateDispatchedTask(Long id, DispatchedTaskTestDTO dispatchedTaskDTO);
    DispatchedTaskTestDTO getDispatchedTaskById(Long id);
    void deleteDispatchedTask(Long id);
}
