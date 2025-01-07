package com.fps.svmes.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fps.svmes.dto.dtos.dispatch.DispatchedTaskDTO;
import com.fps.svmes.models.sql.task_schedule.*;


import com.fasterxml.jackson.databind.ObjectMapper;

import com.fps.svmes.models.sql.user.User;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                map().setUserId(Long.valueOf(source.getUser().getId())); // Map user ID
                map().setQcFormTreeNodeId(source.getQcFormTreeNodeId()); // Map form tree node ID
                map().setDispatchTime(source.getDispatchTime()); // Map dispatch time
                map().setState(source.getState()); // Map state
                map().setStatus(source.getStatus()); // Map status
                map().setNotes(source.getNotes()); // Map notes
                map().setFinishedAt(source.getFinishedAt()); // Map finished_at
                map().setUpdatedAt(source.getUpdatedAt()); // Map updated_at

                // Null-safe mapping for createdBy
                using(ctx -> ctx.getSource() != null ? ((User) ctx.getSource()).getId() : null)
                        .map(source.getCreatedBy(), destination.getCreated_by());

                // Null-safe mapping for updatedBy
                using(ctx -> ctx.getSource() != null ? ((User) ctx.getSource()).getId() : null)
                        .map(source.getUpdatedBy(), destination.getUpdated_by());
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
