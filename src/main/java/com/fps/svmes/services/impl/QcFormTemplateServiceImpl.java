package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;
import com.fps.svmes.models.sql.qcForm.QcFormTemplate;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcFormTemplateRepository;
import com.fps.svmes.services.QcFormTemplateService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QcFormTemplateServiceImpl implements QcFormTemplateService {

    @Autowired
    private QcFormTemplateRepository repository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public List<QcFormTemplateDTO> getAllActiveTemplates() {
        return repository.findAllByStatus(1).stream()
                .map(template -> modelMapper.map(template, QcFormTemplateDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public QcFormTemplateDTO getTemplateById(Long id) {
        QcFormTemplate template = repository.findById(id).orElseThrow(() -> new RuntimeException("Template not found"));
        return modelMapper.map(template, QcFormTemplateDTO.class);
    }

    @Override
    public QcFormTemplateDTO createTemplate(QcFormTemplateDTO dto) {
        QcFormTemplate template = modelMapper.map(dto, QcFormTemplate.class);
        template.setCreatedAt(OffsetDateTime.now());
        template.setStatus(1);
        return modelMapper.map(repository.save(template), QcFormTemplateDTO.class);
    }

    @Override
    public QcFormTemplateDTO updateTemplate(Long id, QcFormTemplateDTO dto) {
        QcFormTemplate template = repository.findById(id).orElseThrow(() -> new RuntimeException("Template not found"));

        if (dto.getName() != null) {
            template.setName(dto.getName());
        }
        if (dto.getFormTemplateJson() != null) {
            template.setFormTemplateJson(dto.getFormTemplateJson());
        } else {
            template.setFormTemplateJson(template.getFormTemplateJson());
        }
        template.setUpdatedAt(OffsetDateTime.now());
        template.setUpdatedBy(dto.getUpdatedBy());

        return modelMapper.map(repository.save(template), QcFormTemplateDTO.class);
    }

    @Override
    public void deleteTemplate(Long id) {
        QcFormTemplate template = repository.findById(id).orElseThrow(() -> new RuntimeException("Template not found"));
        template.setStatus(0);
        repository.save(template);
    }
}
