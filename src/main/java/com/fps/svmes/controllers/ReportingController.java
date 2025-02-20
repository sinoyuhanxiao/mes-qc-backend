package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.reporting.WidgetDataDTO;
import com.fps.svmes.services.ReportingService;
import org.bson.Document;
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
    public List<WidgetDataDTO> extractWidgetDataWithCounts(
            @RequestParam Long formTemplateId,
            @RequestParam(required = false) String startDateTime,
            @RequestParam(required = false) String endDateTime
    ) {
        return reportingService.extractWidgetDataWithCounts(formTemplateId, startDateTime, endDateTime);
    }

    /**
     * Fetch QC records within a given date range, with pagination.
     */
    @GetMapping("/qc-records")
    public List<Document> getQcRecords(
            @RequestParam Long formTemplateId,
            @RequestParam String startDateTime,
            @RequestParam String endDateTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return reportingService.fetchQcRecords(formTemplateId, startDateTime, endDateTime, page, size);
    }

}
