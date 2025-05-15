package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.qcForm.QcApprovalAssignmentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface QcApprovalAssignmentService {
    void insertIfNotExists(QcApprovalAssignmentDTO dto);

    Page<QcApprovalAssignmentDTO> getAllAssignments(Pageable pageable);

    Page<QcApprovalAssignmentDTO> getFilteredAssignments(
            String state,
            String approvalType,
            String templateName,
            String startDate,
            String endDate,
            Pageable pageable
    );
}