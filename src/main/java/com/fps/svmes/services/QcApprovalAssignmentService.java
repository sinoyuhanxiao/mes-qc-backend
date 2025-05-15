package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.qcForm.QcApprovalAssignmentDTO;

public interface QcApprovalAssignmentService {
    void insertIfNotExists(QcApprovalAssignmentDTO dto);
}