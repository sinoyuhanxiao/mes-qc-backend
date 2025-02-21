package com.fps.svmes.dto.dtos.reporting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataDTO {
    private String chartImage; // Base64 Image
    private String chartType;
    private Double min;
    private Double max;
    private Double average;
    private Integer total;
    private List<PieInfoDTO> info;
}

