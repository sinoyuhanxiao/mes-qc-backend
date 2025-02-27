package com.fps.svmes.dto.dtos.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuarterlyTaskStatisticsDTO {
    private Long userId;
    private Map<String, Integer> quarterlyTasks; // e.g., {"Q1": 520, "Q2": 320, "Q3": 740, "Q4": 630}
}
