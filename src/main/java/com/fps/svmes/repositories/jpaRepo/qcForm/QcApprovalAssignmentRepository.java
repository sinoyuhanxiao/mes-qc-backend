package com.fps.svmes.repositories.jpaRepo.qcForm;

import com.fps.svmes.models.sql.qcForm.QcApprovalAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QcApprovalAssignmentRepository extends JpaRepository<QcApprovalAssignment, Long> {
    Optional<QcApprovalAssignment> findBySubmissionId(String submissionId);
}
