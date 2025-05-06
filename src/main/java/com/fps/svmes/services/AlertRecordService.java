package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.alert.AlertRecordDTO;
import com.fps.svmes.dto.dtos.alert.AlertSummaryDTO;
import com.fps.svmes.dto.dtos.alert.DetailedAlertRecordDTO;
import com.fps.svmes.dto.requests.alert.AlertRecordFilterRequest;
import com.fps.svmes.models.sql.alert.AlertRecord;
import org.springframework.data.domain.Page;

public interface AlertRecordService {
    AlertRecordDTO create(AlertRecordDTO dto);
    Page<DetailedAlertRecordDTO> getDetailedList(int page, int size);
    AlertRecordDTO updateRecord(Long alertId, Integer newRpn, Integer userId);
    AlertRecordDTO deleteRecord(Long alertId, Integer userId);
    AlertSummaryDTO getAlertSummary();
    Page<DetailedAlertRecordDTO> filterAlertRecords(AlertRecordFilterRequest request);

}
