package com.fps.svmes.dto.requests;

import com.fps.svmes.dto.dtos.reporting.ChartDataDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    private String startDateTime;
    private String endDateTime;
    private List<ChartDataDTO> charts;
}

