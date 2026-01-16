package com.fps.svmes.repositories.jpaRepo.alert;

import com.fps.svmes.models.sql.alert.AlertRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long>, JpaSpecificationExecutor<AlertRecord> {
    List<AlertRecord> findByStatus(Integer status);
    void deleteBySubmissionId(String submissionId);
    void deleteBySubmissionIdIn(List<String> submissionIds);
}

