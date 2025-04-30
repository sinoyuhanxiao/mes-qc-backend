package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.alert.AlertRecordDTO;

public interface AlertRecordService {
    AlertRecordDTO create(AlertRecordDTO dto);
    AlertRecordDTO updateRecord(Long alertId, Integer newRpn, Integer userId);
    AlertRecordDTO deleteRecord(Long alertId, Integer userId);
}
