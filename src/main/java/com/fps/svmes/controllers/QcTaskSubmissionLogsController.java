package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.qcForm.QcTaskSubmissionLogsDTO;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.services.QcTaskSubmissionLogsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.bson.Document;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/qc-task-submission-logs")
@RequiredArgsConstructor
@Tag(name = "QC Task Submission Logs API", description = "API for managing QC task submission logs")
public class QcTaskSubmissionLogsController {

    private final QcTaskSubmissionLogsService service;
    private static final Logger logger = LoggerFactory.getLogger(QcTaskSubmissionLogsController.class);
    private final QcTaskSubmissionLogsService qcTaskSubmissionLogsService;

    @PostMapping
    @Operation(summary = "Create a new QC task submission log", description = "Creates a new QC task submission log.")
    public ResponseResult<QcTaskSubmissionLogsDTO> createLog(@RequestBody QcTaskSubmissionLogsDTO dto) {
        try {
            QcTaskSubmissionLogsDTO createdLog = service.insertLog(dto);
            logger.info("Log created: {}", createdLog);
            return ResponseResult.success(createdLog);
        } catch (Exception e) {
            logger.error("Error creating log", e);
            return ResponseResult.fail("Error creating log", e);
        }
    }

    @GetMapping
    @Operation(summary = "Get all logs by created_by and dispatched_task_id")
    public ResponseEntity<?> getAllByCreatedByAndDispatchedTaskId(
            @RequestParam Integer createdBy,
            @RequestParam Long dispatchedTaskId) {
        try {
            List<QcTaskSubmissionLogsDTO> logs = service.getAllByCreatedByAndTaskId(createdBy, dispatchedTaskId);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving logs: " + e.getMessage());
        }
    }

    @GetMapping("/document")
    @Operation(summary = "Get Mongo document by submission ID")
    public ResponseEntity<?> getDocument(
            @RequestParam String submissionId,
            @RequestParam Long qcFormTemplateId,
            @RequestParam Integer createdBy) {
        try {
            Document document = qcTaskSubmissionLogsService.getDocumentBySubmissionId(submissionId, qcFormTemplateId, createdBy);
            if (document == null) {
                return ResponseEntity.status(404).body("Document not found for submissionId: " + submissionId);
            }
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving document: " + e.getMessage());
        }
    }

}
