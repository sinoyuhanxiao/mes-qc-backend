package com.fps.svmes.dto.dtos.task;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskStatisticsDTO {
    private Long userId;
    private TaskCountDTO todayTasks;
    private TaskCountDTO futureTasks;
    private TaskCountDTO historicalTasks;
    private TaskCountDTO overdueTasks;
}

