package com.fps.svmes.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
import com.fps.svmes.dto.dtos.dispatch.DispatchedTaskDTO;
import com.fps.svmes.dto.dtos.user.UserDTO;
import com.fps.svmes.models.sql.task_schedule.*;


import com.fasterxml.jackson.databind.ObjectMapper;

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
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setSkipNullEnabled(true);
        
        // Add custom mapping for DispatchedTask to DispatchedTaskDTO
        mapper.addMappings(new PropertyMap<DispatchedTask, DispatchedTaskDTO>() {
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
      
        return mapper;
    }

    @Bean
    public ObjectMapper objectMapper() {
//        return new ObjectMapper(); // Use for JSON serialization/deserialization
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // Support for Java 8 Date/Time types
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Use ISO-8601 format
        return mapper;
    }

}
