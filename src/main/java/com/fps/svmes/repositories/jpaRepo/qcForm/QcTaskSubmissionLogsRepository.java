package com.fps.svmes.repositories.jpaRepo.qcForm;

import com.fps.svmes.models.sql.qcForm.QcTaskSubmissionLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QcTaskSubmissionLogsRepository extends JpaRepository<QcTaskSubmissionLogs, Long> {
    List<QcTaskSubmissionLogs> findAllByCreatedByAndDispatchedTaskId(Integer createdBy, Long dispatchedTaskId);
}

