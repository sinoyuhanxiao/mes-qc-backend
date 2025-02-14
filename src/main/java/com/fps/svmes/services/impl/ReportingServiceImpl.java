package com.fps.svmes.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fps.svmes.dto.dtos.reporting.OptionItemDTO;
import com.fps.svmes.dto.dtos.reporting.WidgetDataDTO;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcFormTemplateRepository;
import com.fps.svmes.services.ReportingService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Accumulators.sum;

@Service
public class ReportingServiceImpl implements ReportingService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MongoClient mongoClient;

    @Autowired
    QcFormTemplateRepository qcFormTemplateRepository;

    @Autowired
    public ReportingServiceImpl(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @Override
    public List<WidgetDataDTO> extractWidgetData(String jsonInput) {
        List<WidgetDataDTO> extractedData = new ArrayList<>();
        parseJsonToWidgetList(jsonInput, extractedData);
        return extractedData;
    }

    @Override
    public List<WidgetDataDTO> extractWidgetDataWithCounts(Long formTemplateId) {
        String jsonInput = qcFormTemplateRepository.findFormTemplateJsonById(formTemplateId);
        List<WidgetDataDTO> widgetDataList = extractWidgetData(jsonInput);
        MongoDatabase database = mongoClient.getDatabase("dev-mes-qc");
        List<String> collectionNames = generateCollectionNames(formTemplateId);

        // Apply updates step by step
        for (String collectionName : collectionNames) {
            widgetDataList = processCollection(database, collectionName, widgetDataList);
        }

        return widgetDataList;
    }

    private void parseJsonToWidgetList(String jsonInput, List<WidgetDataDTO> extractedData) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonInput);
            JsonNode widgetList = rootNode.path("widgetList");

            if (widgetList.isArray()) {
                for (JsonNode widgetNode : widgetList) {
                    extractWidgetRecursive(widgetNode, extractedData);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void extractWidgetRecursive(JsonNode widgetNode, List<WidgetDataDTO> extractedData) {
        String type = widgetNode.path("type").asText();

        JsonNode options = widgetNode.path("options");
        if (!options.isMissingNode()) {
            String name = options.path("name").asText();
            String label = options.path("label").asText();
            JsonNode optionItems = options.path("optionItems");

            List<OptionItemDTO> optionList = new ArrayList<>();
            if (optionItems.isArray()) {
                for (JsonNode item : optionItems) {
                    optionList.add(new OptionItemDTO(
                            item.path("label").asText(),
                            item.path("value").asInt(),
                            0
                    ));
                }
            }

            extractedData.add(new WidgetDataDTO(name, label, type, optionList, null, null));
        }

        JsonNode cols = widgetNode.path("cols");
        if (cols.isArray()) {
            for (JsonNode col : cols) {
                JsonNode widgetList = col.path("widgetList");
                if (widgetList.isArray()) {
                    for (JsonNode nestedWidget : widgetList) {
                        extractWidgetRecursive(nestedWidget, extractedData);
                    }
                }
            }
        }
    }

    // TODO: modify this to enable time filtering
    private List<String> generateCollectionNames(Long formTemplateId) {
        List<String> collectionNames = new ArrayList<>();

        // Get all collections in the database
        MongoDatabase database = mongoClient.getDatabase("dev-mes-qc");
        for (String collectionName : database.listCollectionNames()) {
            // Match collections that follow the "form_template_{formTemplateId}_{YYYYMM}" pattern
            if (collectionName.matches("form_template_" + formTemplateId + "_\\d{6}")) {
                collectionNames.add(collectionName);
            }
        }

        return collectionNames;
    }

    private List<WidgetDataDTO> processCollection(
            MongoDatabase database, String collectionName, List<WidgetDataDTO> widgetDataList
    ) {

        MongoCollection<Document> collection = database.getCollection(collectionName);
        List<WidgetDataDTO> updatedWidgets = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // ✅ Format

        for (WidgetDataDTO widget : widgetDataList) {
            if (widget.getOptionItems().isEmpty() && !widget.getType().equals("number")) {
                updatedWidgets.add(widget);
                continue;
            }

            if (widget.getType().equals("number")) {
                // ✅ Extract numerical data and timestamps
                List<Double> chartData = new ArrayList<>();
                List<String> xaxisData = new ArrayList<>();

                for (Document doc : collection.find()) {
                    if (doc.containsKey(widget.getName())) {
                        Object value = doc.get(widget.getName()); // ✅ Retrieve value dynamically

                        if (value instanceof Integer) {
                            chartData.add(((Integer) value).doubleValue()); // ✅ Convert Integer to Double
                        } else if (value instanceof Double) {
                            chartData.add((Double) value); // ✅ Directly add Double values
                        }
                    }
                    if (doc.containsKey("created_at")) {
                        try {
                            String rawTimestamp = doc.getString("created_at");
                            LocalDateTime dateTime = LocalDateTime.parse(rawTimestamp);
                            xaxisData.add(dateTime.format(formatter)); // ✅ Format timestamp
                        } catch (Exception e) {
                            System.out.println("Error parsing created_at: " + e.getMessage());
                        }
                    }
                }

                updatedWidgets.add(new WidgetDataDTO(widget.getName(), widget.getLabel(), widget.getType(),
                        new ArrayList<>(), chartData, xaxisData));
            } else {
                // ✅ Process option-based widgets (Pie Charts)
                Map<Integer, Integer> valueCountMap = countOptionOccurrences(collection, widget.getName(), widget.getOptionItems());

                List<OptionItemDTO> updatedOptions = widget.getOptionItems().stream()
                        .map(option -> new OptionItemDTO(
                                option.getLabel(),
                                option.getValue(),
                                valueCountMap.getOrDefault(option.getValue(), 0)
                        ))
                        .collect(Collectors.toList());

                updatedWidgets.add(new WidgetDataDTO(widget.getName(), widget.getLabel(), widget.getType(),
                        updatedOptions, null, null));
            }
        }

        return updatedWidgets;
    }


    private Map<String, List<Object>> extractNumberFieldData(
            MongoCollection<Document> collection, String fieldName) {
        List<Object> values = new ArrayList<>();
        List<Object> timestamps = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (Document doc : collection.find(exists(fieldName, true))) {
            if (doc.containsKey(fieldName) && doc.get(fieldName) instanceof Number) {
                values.add(doc.get(fieldName));

                if (doc.containsKey("created_at")) {
                    Object createdAt = doc.get("created_at");

                    if (createdAt instanceof String) {
                        try {
                            LocalDateTime parsedDate = Instant.parse((String) createdAt)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();
                            timestamps.add(parsedDate.toString().replace("T", " ").substring(0, 16));
                        } catch (Exception e) {
                            System.out.println("Invalid date format: " + createdAt);
                            timestamps.add(createdAt); // Store as-is if parsing fails
                        }
                    }
                }
            }
        }

        Map<String, List<Object>> result = new HashMap<>();
        result.put("values", values);
        result.put("timestamps", timestamps);
        return result;
    }

    private Map<Integer, Integer> countOptionOccurrences(
            MongoCollection<Document> collection, String fieldName, List<OptionItemDTO> options) {
        Map<Integer, Integer> countMap = new HashMap<>();

        List<Bson> pipeline = Arrays.asList(
                match(exists(fieldName, true)),   // Only consider documents where the field exists
                unwind("$" + fieldName),         // Flatten arrays into separate documents
                group("$" + fieldName, sum("count", 1)) // Group and count occurrences
        );

        for (Document doc : collection.aggregate(pipeline)) {
            Object value = doc.get("_id");
            Integer count = doc.getInteger("count");

            if (value instanceof Integer) {
                countMap.put((Integer) value, count);
            } else if (value instanceof String) {
                try {
                    countMap.put(Integer.parseInt((String) value), count);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid integer conversion for _id: " + value);
                }
            }
        }

        return countMap;
    }
}
