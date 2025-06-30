package com.fps.svmes.services;

import com.fps.svmes.dto.PagedResultDTO;
import com.fps.svmes.dto.dtos.reporting.WidgetDataDTO;
import org.bson.Document;

import java.security.Timestamp;
import java.util.List;

public interface ReportingService {
    List<WidgetDataDTO> extractWidgetData(String jsonInput);

    // Now receives jsonInput instead of a list of WidgetDataDTO
    List<WidgetDataDTO> extractWidgetDataWithCounts(Long formTemplateId, String startDateTime, String endDateTime);

    // Old fetchQcRecords method without pagination
    List<Document> fetchQcRecords(Long formTemplateId, String startDateTime, String endDateTime, Integer page, Integer size);

    List<Document> fetchQcRecordsFilteredByCreator(Long formTemplateId, String startDateTime, String endDateTime, Integer page, Integer size, Integer createdBy);

    List<Document> fetchAllVersionsByGroupId(Long formTemplateId, String versionGroupId);

    PagedResultDTO<Document> fetchQcRecordsPaged(
            Long formTemplateId,
            String startDateTime,
            String endDateTime,
            Integer page,
            Integer size,
            String sort,
            String search
    );

    List<Document> fetchAllRecordsWithoutPagination(Long formTemplateId,
                                                           String startDateTime,
                                                           String endDateTime,
                                                           String search,
                                                           String sort);
}

