package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.alert.AlertRecordDTO;
import com.fps.svmes.dto.dtos.alert.DetailedAlertRecordDTO;
import org.springframework.data.domain.Page;

public interface AlertRecordService {
    AlertRecordDTO create(AlertRecordDTO dto);
    Page<DetailedAlertRecordDTO> getDetailedList(int page, int size);
    AlertRecordDTO updateRecord(Long alertId, Integer newRpn, Integer userId);
    AlertRecordDTO deleteRecord(Long alertId, Integer userId);
}
