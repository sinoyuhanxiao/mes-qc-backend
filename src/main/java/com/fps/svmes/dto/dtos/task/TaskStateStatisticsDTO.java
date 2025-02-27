package com.fps.svmes.dto.dtos.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskStateStatisticsDTO {
    private Long userId;
    private int pendingCount;
    private int inProgressCount;
    private int completedCount;
}
