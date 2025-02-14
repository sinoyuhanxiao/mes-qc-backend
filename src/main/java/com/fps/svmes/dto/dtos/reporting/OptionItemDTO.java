package com.fps.svmes.dto.dtos.reporting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptionItemDTO {
    private String label;
    private int value;
    private int count;
}
