package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.reporting.WidgetDataDTO;
import com.fps.svmes.services.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reporting")
public class ReportingController {

    private final ReportingService reportingService;

    @Autowired
    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @PostMapping("/extract")
    public List<WidgetDataDTO> extractWidgetData(@RequestBody String jsonInput) {
        return reportingService.extractWidgetData(jsonInput);
    }

    @PostMapping("/extract-with-counts")
    public List<WidgetDataDTO> extractWidgetDataWithCounts(@RequestParam Long formTemplateId) {
        return reportingService.extractWidgetDataWithCounts(formTemplateId);
    }

}
