package com.fps.svmes.controllers;

import com.fps.svmes.dto.PagedResultDTO;
import com.fps.svmes.dto.dtos.reporting.WidgetDataDTO;
import com.fps.svmes.services.ReportingService;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public PagedResultDTO<Document> getQcRecords(
            @RequestParam Long formTemplateId,
            @RequestParam String startDateTime,
            @RequestParam String endDateTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search
    ) {
        return reportingService.fetchQcRecordsPaged(
                formTemplateId,
                startDateTime,
                endDateTime,
                page,
                size,
                sort,
                search
        );
    }

    @GetMapping("/qc-records/export")
    public ResponseEntity<List<Document>> exportQcRecords(
            @RequestParam Long   formTemplateId,
            @RequestParam String startDateTime,
            @RequestParam String endDateTime,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort) {

        List<Document> allRecords = reportingService.fetchAllRecordsWithoutPagination(
                formTemplateId, startDateTime, endDateTime, search, sort);

        return ResponseEntity.ok(allRecords);
    }


    /**
     * Fetch QC records filtered by created_by ID, with pagination.
     */
    @GetMapping("/qc-records/by-user")
    public List<Document> getQcRecordsByUser(
            @RequestParam Long formTemplateId,
            @RequestParam String startDateTime,
            @RequestParam String endDateTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Integer createdBy) {

        return reportingService.fetchQcRecordsFilteredByCreator(formTemplateId, startDateTime, endDateTime, page, size, createdBy);
    }

    @GetMapping("/qc-records/versions")
    public List<Document> getAllVersionsByGroupId(
            @RequestParam Long formTemplateId,
            @RequestParam String versionGroupId
    ) {
        return reportingService.fetchAllVersionsByGroupId(formTemplateId, versionGroupId);
    }


}
