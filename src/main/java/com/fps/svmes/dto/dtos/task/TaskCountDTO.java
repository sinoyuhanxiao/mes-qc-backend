package com.fps.svmes.dto.dtos.task;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskCountDTO {
    private int count;
    private int percentageChange;
}
