package com.fps.svmes.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fps.svmes.dto.dtos.reporting.OptionItemDTO;
import com.fps.svmes.dto.dtos.reporting.WidgetDataDTO;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcFormTemplateRepository;
import com.fps.svmes.services.ReportingService;
import com.fps.svmes.services.UserService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
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
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    QcFormTemplateRepository qcFormTemplateRepository;

    @Autowired
    UserService userService;

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

                }
            }
        }

        return countMap;
    }

    /**
     * Fetch QC records for a given template ID within a date range, with pagination.
     */
    @Override
    public List<Document> fetchQcRecords(Long formTemplateId, String startDateTime, String endDateTime, Integer page, Integer size) {
        MongoDatabase database = mongoClient.getDatabase("dev-mes-qc");

        // ✅ Get label mappings for formTemplateId
        HashMap<String, Object> optionItemsKeyValueMap = QcFormTemplateOptionItemsKeyValueMapping(formTemplateId);
        HashMap<String, String> keyValueMap = getFormTemplateKeyValueMapping(formTemplateId);

        // ✅ Get target collections based on the date range
        List<String> collectionNames = getRelevantCollections(database, formTemplateId, startDateTime, endDateTime);

        List<Document> records = new ArrayList<>();
        for (String collectionName : collectionNames) {
            MongoCollection<Document> collection = database.getCollection(collectionName);
            records.addAll(queryRecords(collection, startDateTime, endDateTime, page, size));
        }

        // ✅ Convert keys and values before returning
        return records.stream()
                .map(doc -> formattedResult(doc, optionItemsKeyValueMap, keyValueMap))
                .collect(Collectors.toList());
    }

    public HashMap<String, String> getFormTemplateKeyValueMapping(Long formId) {
        String formTemplateJson = qcFormTemplateRepository.findFormTemplateJsonById(formId);

        if (formTemplateJson == null || formTemplateJson.isEmpty()) {
            throw new RuntimeException("Form template JSON not found for formId: " + formId);
        }

        HashMap<String, String> keyValueMap = new HashMap<>();

        try {
            Document formTemplate = Document.parse(formTemplateJson);
            List<Document> widgetList = (List<Document>) formTemplate.get("widgetList");

            if (widgetList != null) {
                extractKeyValuePairs(widgetList, keyValueMap); // ✅ Extract key-label mappings
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing form template JSON", e);
        }

        return keyValueMap;
    }

    private void extractKeyValuePairs(List<Document> widgetList, HashMap<String, String> keyValueMap) {
        for (Document widget : widgetList) {
            // ✅ Extract options and add name-label pairs to the map
            Document options = (Document) widget.get("options");
            if (options != null) {
                String name = options.getString("name");
                String label = options.getString("label");
                if (name != null && label != null) {
                    keyValueMap.put(name, label);
                }
            }

            // ✅ Recursively process nested widgetList
            List<Document> nestedWidgetList = (List<Document>) widget.get("widgetList");
            if (nestedWidgetList != null) {
                extractKeyValuePairs(nestedWidgetList, keyValueMap);
            }

            // ✅ Check for widget lists inside grid columns
            List<Document> cols = (List<Document>) widget.get("cols");
            if (cols != null) {
                for (Document col : cols) {
                    List<Document> colWidgetList = (List<Document>) col.get("widgetList");
                    if (colWidgetList != null) {
                        extractKeyValuePairs(colWidgetList, keyValueMap);
                    }
                }
            }
        }
    }


    private Document formattedResult(Document document, HashMap<String, Object> optionItemsKeyValueMap, HashMap<String, String> keyValueMap) {
        Document formattedDocument = new Document();

        for (String key : document.keySet()) {
            Object value = document.get(key);

            // ✅ 确保 `_id` 仍然是 ObjectId 而不是 timestamp + date 结构
            if ("_id".equals(key) && value instanceof ObjectId) {
                formattedDocument.put("_id", value.toString()); // 转换成字符串
                continue;
            }

            // ✅ 替换字段名
            String formattedKey = keyValueMap.getOrDefault(key, key);

            // ✅ 如果 value 是选项列表，则转换成对应的 label
            if (optionItemsKeyValueMap.containsKey(key) && value instanceof List) {
                List<?> valueList = (List<?>) value;
                HashMap<String, String> valueToLabelMap = (HashMap<String, String>) optionItemsKeyValueMap.get(key);

                List<String> resolvedLabels = valueList.stream()
                        .map(val -> valueToLabelMap.getOrDefault(val.toString(), val.toString()))
                        .collect(Collectors.toList());
                formattedDocument.put(formattedKey, resolvedLabels);
            }
            // ✅ 如果 value 是单个数值并且有 label 映射，则转换
            else if (optionItemsKeyValueMap.containsKey(key) && (value instanceof Integer || value instanceof String)) {
                HashMap<String, String> valueToLabelMap = (HashMap<String, String>) optionItemsKeyValueMap.get(key);
                formattedDocument.put(formattedKey, valueToLabelMap.getOrDefault(value.toString(), value.toString()));
            } else {
                formattedDocument.put(formattedKey, value);
            }

            // ✅ 获取 `created_by` 并添加 `created_by_name`
            if (document.containsKey("created_by") && document.get("created_by") instanceof Long) {
                Long createdById = document.getLong("created_by"); // ✅ 直接获取 Long 类型
                try {
                    String creatorName = userService.getUserById(Math.toIntExact(createdById)).getName(); // ✅ Long 转 Integer
                    formattedDocument.put("提交人", creatorName); // 添加 `created_by_name`
                } catch (ArithmeticException e) {
                    formattedDocument.put("created_by_name", "ID 超出 Integer 范围");
                } catch (Exception e) {
                    formattedDocument.put("提交人", "未知用户"); // 兜底方案
                }
            }
        }

        return formattedDocument;
    }

    private HashMap<String, Object> QcFormTemplateOptionItemsKeyValueMapping(Long formId) {
        String formTemplateJson = qcFormTemplateRepository.findFormTemplateJsonById(formId);

        if (formTemplateJson == null || formTemplateJson.isEmpty()) {
            throw new RuntimeException("Form template JSON not found for formId: " + formId);
        }

        HashMap<String, Object> optionItemsKeyValueMap = new HashMap<>();

        try {
            Document formTemplate = Document.parse(formTemplateJson);
            List<Document> widgetList = (List<Document>) formTemplate.get("widgetList");

            if (widgetList != null) {
                extractOptionItems(widgetList, optionItemsKeyValueMap);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing form template JSON", e);
        }

        return optionItemsKeyValueMap;
    }

    private void extractOptionItems(List<Document> widgetList, HashMap<String, Object> optionItemsKeyValueMap) {
        for (Document widget : widgetList) {
            // Extract options with optionItems
            Document options = (Document) widget.get("options");
            if (options != null) {
                String name = options.getString("name");
                List<Document> optionItems = (List<Document>) options.get("optionItems");

                if (name != null && optionItems != null) {
                    HashMap<String, String> valueToLabelMap = new HashMap<>();
                    for (Document option : optionItems) {
                        Object value = option.get("value");
                        String optionLabel = option.getString("label");
                        if (value != null && optionLabel != null) {
                            valueToLabelMap.put(value.toString(), optionLabel);
                        }
                    }
                    optionItemsKeyValueMap.put(name, valueToLabelMap);
                }
            }

            // Recursively process nested widgetList
            List<Document> nestedWidgetList = (List<Document>) widget.get("widgetList");
            if (nestedWidgetList != null) {
                extractOptionItems(nestedWidgetList, optionItemsKeyValueMap);
            }

            // Check for widget lists inside grid columns
            List<Document> cols = (List<Document>) widget.get("cols");
            if (cols != null) {
                for (Document col : cols) {
                    List<Document> colWidgetList = (List<Document>) col.get("widgetList");
                    if (colWidgetList != null) {
                        extractOptionItems(colWidgetList, optionItemsKeyValueMap);
                    }
                }
            }
        }
    }

    /**
     * Identify the collections relevant to the given date range.
     */
    private List<String> getRelevantCollections(MongoDatabase database, Long formTemplateId, String startDateTime, String endDateTime) {
        List<String> targetCollections = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");

        LocalDateTime start = LocalDateTime.parse(startDateTime, DATE_FORMATTER);
        LocalDateTime end = LocalDateTime.parse(endDateTime, DATE_FORMATTER);

        while (!start.isAfter(end)) {
            String collectionName = "form_template_" + formTemplateId + "_" + sdf.format(java.sql.Timestamp.valueOf(start));
            if (database.listCollectionNames().into(new ArrayList<>()).contains(collectionName)) {
                targetCollections.add(collectionName);
            }
            start = start.plusMonths(1); // Move to next month
        }
        return targetCollections;
    }

    /**
     * Query MongoDB collection for records within the time range and apply pagination.
     */
    private List<Document> queryRecords(MongoCollection<Document> collection, String startDateTime, String endDateTime, int page, int size) {
        List<Bson> filters = Arrays.asList(
                gte("created_at", startDateTime),
                lte("created_at", endDateTime)
        );

        return collection.find(and(filters))
                .skip(page * size)  // Pagination: skip past (page * size) records
                .limit(size)        // Limit results to `size` per page
                .into(new ArrayList<>());
    }

}
