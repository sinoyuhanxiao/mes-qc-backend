package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.qcForm.QcTaskSubmissionLogsDTO;

import org.bson.Document;
import java.util.List;

public interface QcTaskSubmissionLogsService {
    QcTaskSubmissionLogsDTO insertLog(QcTaskSubmissionLogsDTO dto);
    List<QcTaskSubmissionLogsDTO> getAllByCreatedByAndTaskId(Integer createdBy, Long dispatchedTaskId);

    Document getDocumentBySubmissionId(String submissionId, Long formId, Integer createdBy);
}