package com.fps.svmes.dto.dtos.reporting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PieInfoDTO {
    private String label;
    private Integer count;
    private Double percentage;
}