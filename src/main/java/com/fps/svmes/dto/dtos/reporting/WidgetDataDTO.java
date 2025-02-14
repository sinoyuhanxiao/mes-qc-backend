package com.fps.svmes.dto.dtos.reporting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WidgetDataDTO {
    private String name;
    private String label;
    private String type;
    private List<OptionItemDTO> optionItems;
    private List<Double> chartData; // Holds numeric data
    private List<String> xaxisData; // Holds formatted timestamps
}
