package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.qcForm.QcApprovalAssignmentDTO;
import com.fps.svmes.models.sql.qcForm.QcApprovalAssignment;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcApprovalAssignmentRepository;
import com.fps.svmes.services.QcApprovalAssignmentService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QcApprovalAssignmentServiceImpl implements QcApprovalAssignmentService {

    private final QcApprovalAssignmentRepository repository;
    private final ModelMapper modelMapper;

    @Override
    public void insertIfNotExists(QcApprovalAssignmentDTO dto) {
        Optional<QcApprovalAssignment> existing = repository.findBySubmissionId(dto.getSubmissionId());
        if (existing.isEmpty()) {
            QcApprovalAssignment entity = modelMapper.map(dto, QcApprovalAssignment.class);
            repository.save(entity);
        }
    }
}
