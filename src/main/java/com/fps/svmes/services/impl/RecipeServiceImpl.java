package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.recipe.ControlLimitSettingDTO;
import com.fps.svmes.models.nosql.ControlLimitSetting;
import com.fps.svmes.repositories.mongoRepo.ControlLimitSettingRepository;
import com.fps.svmes.services.RecipeService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;

@Service
public class RecipeServiceImpl implements RecipeService {

    @Autowired
    private ControlLimitSettingRepository repository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ControlLimitSettingDTO getByQcFormTemplateId(Long templateId) {
        return repository.findByQcFormTemplateId(templateId)
                .map(setting -> modelMapper.map(setting, ControlLimitSettingDTO.class))
                .orElse(null);
    }

    @Override
    public void updateControlLimits(ControlLimitSettingDTO dto) {
        ControlLimitSetting setting = repository.findByQcFormTemplateId(dto.getQcFormTemplateId())
                .orElse(new ControlLimitSetting());
        setting.setQcFormTemplateId(dto.getQcFormTemplateId());

        // Use LinkedHashMap to preserve order
        setting.setControlLimits(modelMapper.map(
                dto.getControlLimits(),
                new org.modelmapper.TypeToken<LinkedHashMap<String, ControlLimitSetting.ControlLimitEntry>>() {}.getType()
        ));

        repository.save(setting);
    }

}