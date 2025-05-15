package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.qcForm.QcApprovalAssignmentDTO;
import com.fps.svmes.models.sql.qcForm.QcApprovalAssignment;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcApprovalAssignmentRepository;
import com.fps.svmes.services.QcApprovalAssignmentService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QcApprovalAssignmentServiceImpl implements QcApprovalAssignmentService {

    private final QcApprovalAssignmentRepository repository;
    private final ModelMapper modelMapper;
    private final QcApprovalAssignmentRepository qcApprovalAssignmentRepository;

    @Override
    public void insertIfNotExists(QcApprovalAssignmentDTO dto) {
        Optional<QcApprovalAssignment> existing = repository.findBySubmissionId(dto.getSubmissionId());
        if (existing.isEmpty()) {
            QcApprovalAssignment entity = modelMapper.map(dto, QcApprovalAssignment.class);
            repository.save(entity);
        }
    }

    @Override
    public Page<QcApprovalAssignmentDTO> getAllAssignments(Pageable pageable) {
        return repository.findAll(pageable)
                .map(entity -> modelMapper.map(entity, QcApprovalAssignmentDTO.class));
    }

    @Override
    public Page<QcApprovalAssignmentDTO> getFilteredAssignments(
            String state,
            String approvalType,
            String templateName,
            String startDate,
            String endDate,
            Pageable pageable) {

        Specification<QcApprovalAssignment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(state)) {
                predicates.add(cb.equal(root.get("state"), state));
            }
            if (StringUtils.hasText(approvalType)) {
                predicates.add(cb.equal(root.get("approvalType"), approvalType));
            }
            if (StringUtils.hasText(templateName)) {
                predicates.add(cb.like(root.get("qcFormTemplateName"), "%" + templateName + "%"));
            }
            if (StringUtils.hasText(startDate) && StringUtils.hasText(endDate)) {
                predicates.add(cb.between(root.get("createdAt"),
                        LocalDate.parse(startDate).atStartOfDay(),
                        LocalDate.parse(endDate).atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return qcApprovalAssignmentRepository.findAll(spec, pageable)
                .map(entity -> modelMapper.map(entity, QcApprovalAssignmentDTO.class));
    }

}
