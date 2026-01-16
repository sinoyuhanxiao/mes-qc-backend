package com.fps.svmes.services.impl;

import com.fps.svmes.models.sql.qcForm.QcSnapshotSubmission;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcSnapshotSubmissionRepository;
import com.fps.svmes.services.QcSnapshotSubmissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class QcSnapshotSubmissionServiceImpl implements QcSnapshotSubmissionService {

    @Autowired
    private QcSnapshotSubmissionRepository repository;

    @Override
    @Transactional
    public void updateSubmissionId(String oldSubmissionId, String newSubmissionId) {
        QcSnapshotSubmission submission = repository.findBySubmissionId(oldSubmissionId);
        if (submission != null) {
            submission.setSubmissionId(newSubmissionId);
            repository.save(submission);
            log.info("Updated QcSnapshotSubmission from {} to {}", oldSubmissionId, newSubmissionId);
        } else {
            log.warn("QcSnapshotSubmission not found for submissionId: {}", oldSubmissionId);
        }
    }

    @Override
    @Transactional
    public void deleteBySubmissionId(String submissionId) {
        repository.deleteBySubmissionId(submissionId);
        log.info("Deleted QcSnapshotSubmission for submissionId: {}", submissionId);
    }

    @Override
    @Transactional
    public void deleteBySubmissionIds(List<String> submissionIds) {
        if (submissionIds != null && !submissionIds.isEmpty()) {
            repository.deleteBySubmissionIds(submissionIds);
            log.info("Deleted QcSnapshotSubmissions for submissionIds: {}", submissionIds);
        }
    }
}
