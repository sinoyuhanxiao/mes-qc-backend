package com.fps.svmes.models.sql.task_schedule;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts a List<Long> to a PostgreSQL bigint[] and vice versa.
 */
@Converter
public class LongListConverter implements AttributeConverter<List<Long>, String> {

    @Override
    public String convertToDatabaseColumn(List<Long> attribute) {
        System.out.println("Converting to DB column: " + attribute);
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "{", "}"));
    }

    @Override
    public List<Long> convertToEntityAttribute(String dbData) {
        System.out.println("Converting from DB column: " + dbData);
        if (dbData == null || dbData.isEmpty() || dbData.equals("{}")) {
            return List.of();
        }
        return Arrays.stream(dbData.replace("{", "").replace("}", "").split(","))
                .map(String::trim)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

}
