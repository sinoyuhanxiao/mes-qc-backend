package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.qcForm.QcApprovalAssignmentDTO;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.services.QcApprovalAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.bson.Document;

import java.util.List;

@RestController
@RequestMapping("/approval")
@Tag(name = "QC Approval Assignment API", description = "API for viewing QC approval assignments")
@RequiredArgsConstructor
public class ApprovalController {

    private final QcApprovalAssignmentService approvalAssignmentService;

    private static final Logger logger = LoggerFactory.getLogger(ApprovalController.class);

    /**
     * Get all approval assignments with pagination.
     *
     * @param page Page number (starting from 0)
     * @param size Page size (default 10)
     * @return paginated QcApprovalAssignmentDTO list
     */
    @GetMapping("/assignments")
    @Operation(summary = "Get paginated approval assignments", description = "Returns paginated list of QC approval assignments")
    public ResponseResult<Page<QcApprovalAssignmentDTO>> getAllApprovalAssignments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<QcApprovalAssignmentDTO> result = approvalAssignmentService.getAllAssignments(pageable);
            return ResponseResult.success(result);
        } catch (Exception e) {
            logger.error("Error retrieving approval assignments", e);
            return ResponseResult.fail("Failed to retrieve approval assignments", e);
        }
    }

    @GetMapping("/assignments-filter")
    @Operation(summary = "Get paginated approval assignments", description = "Returns paginated list of QC approval assignments")
    public ResponseResult<Page<QcApprovalAssignmentDTO>> getFilteredApprovalAssignments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String approvalType,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<QcApprovalAssignmentDTO> result = approvalAssignmentService.getFilteredAssignments(
                    state, approvalType, templateName, startDate, endDate, pageable
            );
            return ResponseResult.success(result);
        } catch (Exception e) {
            logger.error("Error retrieving approval assignments", e);
            return ResponseResult.fail("Failed to retrieve approval assignments", e);
        }
    }

    @GetMapping("/version-history")
    @Operation(summary = "Get version history by submission ID", description = "Returns all versions of a submission sorted by version desc")
    public ResponseResult<List<Document>> getVersionHistoryBySubmissionId(
            @RequestParam String submissionId,
            @RequestParam String collectionName
            ) {
        try {
            List<Document> versions = approvalAssignmentService.getVersionHistory(submissionId, collectionName);
            return ResponseResult.success(versions);
        } catch (Exception e) {
            logger.error("Error retrieving version history", e);
            return ResponseResult.fail("Failed to retrieve version history", e);
        }
    }

}
