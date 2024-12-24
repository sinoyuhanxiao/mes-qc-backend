package com.fps.svmes.config;

import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
import com.fps.svmes.dto.dtos.dispatch.DispatchedTaskDTO;
import com.fps.svmes.dto.dtos.user.UserDTO;
import com.fps.svmes.models.sql.task_schedule.*;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hibernate.Hibernate.map;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // Add custom mapping for DispatchedTask to DispatchedTaskDTO
        modelMapper.addMappings(new PropertyMap<DispatchedTask, DispatchedTaskDTO>() {
            @Override
            protected void configure() {
                map().setDispatchId(source.getDispatch().getId()); // Map nested dispatch ID
                map().setPersonnelId(source.getPersonnelId());
                map().setFormId(source.getFormId());
                map().setDispatchTime(source.getDispatchTime());
                map().setStatus(source.getStatus());
                map().setNotes(source.getNotes());
                map().setUpdatedAt(source.getUpdatedAt());
            }
        });

        return modelMapper;
    }

}
