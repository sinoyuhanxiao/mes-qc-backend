package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.alert.AlertRecordDTO;
import com.fps.svmes.models.sql.alert.AlertRecord;
import com.fps.svmes.models.sql.alert.AlertRecordLog;
import com.fps.svmes.repositories.jpaRepo.alert.AlertRecordLogRepository;
import com.fps.svmes.repositories.jpaRepo.alert.AlertRecordRepository;
import com.fps.svmes.services.AlertRecordService;
import com.fps.svmes.utils.AlertDiffBuilder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlertRecordServiceImpl implements AlertRecordService {

    private final AlertRecordRepository alertRecordRepository;
    private final ModelMapper modelMapper;
    private final AlertRecordLogRepository alertRecordLogRepository;

    @Override
    @Transactional
    public AlertRecordDTO create(AlertRecordDTO dto) {
        AlertRecord entity = modelMapper.map(dto, AlertRecord.class);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        AlertRecord saved = alertRecordRepository.save(entity);

        // Create full snapshot diff for create operation
        Map<String, List<String>> diff = new HashMap<>();
        if (dto.getRpn() != null) {
            diff.put("rpn", List.of("-", String.valueOf(dto.getRpn())));
        }
        if (dto.getAlertStatus() != null) {
            diff.put("alert_status", List.of("-", String.valueOf(dto.getAlertStatus())));
        }

        if (!diff.isEmpty()) {
            AlertRecordLog log = new AlertRecordLog();
            log.setAlertRecordId(saved.getId());
            log.setOperation("create");
            log.setDiff(diff);
            log.setCreatedBy(dto.getCreatedBy());
            log.setUpdatedBy(dto.getCreatedBy());
            log.setCreatedAt(OffsetDateTime.now());
            log.setUpdatedAt(OffsetDateTime.now());
            alertRecordLogRepository.save(log);
        }

        return modelMapper.map(saved, AlertRecordDTO.class);
    }

    @Override
    @Transactional
    public AlertRecordDTO updateRecord(Long alertId, Integer newRpn, Integer userId) {
        AlertRecord entity = alertRecordRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("alert record not exist"));

        AlertRecordDTO oldDto = modelMapper.map(entity, AlertRecordDTO.class);

        int newStatus = newRpn < 100 ? 1 : 0;

        entity.setRpn(newRpn);
        entity.setAlertStatus(newStatus);
        entity.setUpdatedBy(userId);
        entity.setUpdatedAt(OffsetDateTime.now());

        alertRecordRepository.save(entity);

        AlertRecordDTO newDto = modelMapper.map(entity, AlertRecordDTO.class);
        Map<String, List<String>> diff = AlertDiffBuilder.buildDiff(oldDto, newDto);

        if (!diff.isEmpty()) {
            AlertRecordLog log = new AlertRecordLog();
            log.setAlertRecordId(alertId);
            log.setOperation("update");
            log.setDiff(diff);
            log.setCreatedBy(userId);
            log.setUpdatedBy(userId);
            log.setCreatedAt(OffsetDateTime.now());
            log.setUpdatedAt(OffsetDateTime.now());
            alertRecordLogRepository.save(log);
        }

        return modelMapper.map(entity, AlertRecordDTO.class);
    }

    @Override
    @Transactional
    public AlertRecordDTO deleteRecord(Long alertId, Integer userId) {
        AlertRecord entity = alertRecordRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("alert record not exist"));

        AlertRecordDTO oldDto = modelMapper.map(entity, AlertRecordDTO.class);

        // Logical deletion (archive)
        entity.setStatus(0);
        entity.setUpdatedBy(userId);
        entity.setUpdatedAt(OffsetDateTime.now());

        alertRecordRepository.save(entity);

        // Logging
        Map<String, List<String>> diff = new HashMap<>();
        diff.put("status", List.of("1", "0")); // active → archived

        AlertRecordLog log = new AlertRecordLog();
        log.setAlertRecordId(alertId);
        log.setOperation("delete");
        log.setDiff(diff);
        log.setCreatedBy(userId);
        log.setUpdatedBy(userId);
        log.setCreatedAt(OffsetDateTime.now());
        log.setUpdatedAt(OffsetDateTime.now());

        alertRecordLogRepository.save(log);

        return modelMapper.map(entity, AlertRecordDTO.class);
    }

}
