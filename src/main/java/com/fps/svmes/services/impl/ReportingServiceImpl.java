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

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
    private static final DateTimeFormatter MONGO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");

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
    public List<WidgetDataDTO> extractWidgetDataWithCounts(Long formTemplateId, String startDateTime, String endDateTime) {
        // Set default start and end timestamps
        Timestamp defaultStart = Timestamp.valueOf(startDateTime);
        Timestamp defaultEnd = Timestamp.valueOf(endDateTime);

        String jsonInput = qcFormTemplateRepository.findFormTemplateJsonById(formTemplateId);
        List<WidgetDataDTO> widgetDataList = extractWidgetData(jsonInput);
        MongoDatabase database = mongoClient.getDatabase("dev-mes-qc");

        // Use updated generateCollectionNames with default timestamps
        List<String> collectionNames = generateCollectionNames(formTemplateId, defaultStart, defaultEnd);

        // Apply updates step by step
        for (String collectionName : collectionNames) {
            List<WidgetDataDTO> newData = processCollection(database, collectionName, widgetDataList, defaultStart, defaultEnd);
            mergeWidgetDataLists(widgetDataList, newData); // Merge instead of overwriting
        }

        return widgetDataList;
    }

    private void mergeWidgetDataLists(List<WidgetDataDTO> originalList, List<WidgetDataDTO> newList) {
        Map<String, WidgetDataDTO> widgetMap = originalList.stream()
                .collect(Collectors.toMap(WidgetDataDTO::getName, w -> w, (w1, w2) -> w1));

        for (WidgetDataDTO newWidget : newList) {
            WidgetDataDTO existingWidget = widgetMap.get(newWidget.getName());

            if (existingWidget != null) {
                if (newWidget.getType().equals("number")) {
                    if (existingWidget.getChartData() == null) {
                        existingWidget.setChartData(new ArrayList<>());
                    }
                    if (existingWidget.getXaxisData() == null) {
                        existingWidget.setXaxisData(new ArrayList<>());
                    }

                    existingWidget.getChartData().addAll(newWidget.getChartData());
                    existingWidget.getXaxisData().addAll(newWidget.getXaxisData());

                    List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>();
                    int size = Math.min(existingWidget.getXaxisData().size(), existingWidget.getChartData().size());
                    for (int i = 0; i < size; i++) {
                        sortedEntries.add(Map.entry(existingWidget.getXaxisData().get(i), existingWidget.getChartData().get(i)));
                    }
                    sortedEntries.sort(Map.Entry.comparingByKey());

                    existingWidget.setXaxisData(sortedEntries.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
                    existingWidget.setChartData(sortedEntries.stream().map(Map.Entry::getValue).collect(Collectors.toList()));

                } else {
                    if (existingWidget.getOptionItems() == null) {
                        existingWidget.setOptionItems(new ArrayList<>());
                    }
                    if (newWidget.getOptionItems() == null) {
                        newWidget.setOptionItems(new ArrayList<>());
                    }

                    // ✅ Corrected: Sum up counts across multiple collections properly
                    Map<Integer, Integer> countMap = new HashMap<>();
                    Map<Integer, String> valueToLabelMap = new HashMap<>();

                    for (OptionItemDTO item : existingWidget.getOptionItems()) {
                        countMap.put(item.getValue(), item.getCount());
                        valueToLabelMap.put(item.getValue(), item.getLabel());
                    }

                    for (OptionItemDTO item : newWidget.getOptionItems()) {
                        countMap.put(item.getValue(), countMap.getOrDefault(item.getValue(), 0) + item.getCount());
                        valueToLabelMap.putIfAbsent(item.getValue(), item.getLabel());
                    }

                    // ✅ Ensuring counts are correctly accumulated across months
                    List<OptionItemDTO> updatedOptions = countMap.entrySet().stream()
                            .map(e -> new OptionItemDTO(valueToLabelMap.get(e.getKey()), e.getKey(), e.getValue()))
                            .collect(Collectors.toList());

                    existingWidget.setOptionItems(updatedOptions);
                }
            } else {
                originalList.add(newWidget);
            }
        }
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

    private List<String> generateCollectionNames(Long formTemplateId, Timestamp utcStartDateTime, Timestamp utcEndDateTime) {
        List<String> collectionNames = new ArrayList<>();
        MongoDatabase database = mongoClient.getDatabase("dev-mes-qc");

        // Convert timestamps to YYYYMM format
        SimpleDateFormat yearMonthFormat = new SimpleDateFormat("yyyyMM");
        int startYearMonth = Integer.parseInt(yearMonthFormat.format(utcStartDateTime));
        int endYearMonth = Integer.parseInt(yearMonthFormat.format(utcEndDateTime));

        for (String collectionName : database.listCollectionNames()) {
            // Extract YYYYMM from collection name
            String pattern = "form_template_" + formTemplateId + "_(\\d{6})";
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(collectionName);

            if (matcher.matches()) {
                int collectionYearMonth = Integer.parseInt(matcher.group(1));

                // Check if collection is within the time range
                if (collectionYearMonth >= startYearMonth && collectionYearMonth <= endYearMonth) {
                    collectionNames.add(collectionName);
                }
            }
        }

        return collectionNames;
    }

    private List<WidgetDataDTO> processCollection(
            MongoDatabase database,
            String collectionName,
            List<WidgetDataDTO> widgetDataList,
            Timestamp startDateTime,
            Timestamp endDateTime
    ) {
        MongoCollection<Document> collection = database.getCollection(collectionName);
        List<WidgetDataDTO> updatedWidgets = new ArrayList<>();

        // Update formatter to support nanoseconds (9-digit precision)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");

        for (WidgetDataDTO widget : widgetDataList) {
            if (widget.getOptionItems().isEmpty() && !widget.getType().equals("number")) {
                updatedWidgets.add(widget);
                continue;
            }

            if (widget.getType().equals("number")) {
                List<Double> chartData = new ArrayList<>();
                List<String> xaxisData = new ArrayList<>();

                for (Document doc : collection.find()) {
                    Date rawDate = doc.getDate("created_at");
                    Instant createdAt = rawDate != null ? rawDate.toInstant() : null;

                    if (createdAt != null &&
                            !createdAt.isBefore(startDateTime.toInstant()) &&
                            !createdAt.isAfter(endDateTime.toInstant())) {

                        if (doc.containsKey(widget.getName())) {
                            Object value = doc.get(widget.getName());
                            if (value instanceof Integer) {
                                chartData.add(((Integer) value).doubleValue());
                            } else if (value instanceof Double) {
                                chartData.add((Double) value);
                            }
                        }

                        // 格式化时间为 "yyyy-MM-dd HH:mm:ss"
                        String formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                .withZone(ZoneId.systemDefault())
                                .format(createdAt);
                        xaxisData.add(formattedDate);
                    }
                }
                updatedWidgets.add(new WidgetDataDTO(widget.getName(), widget.getLabel(), widget.getType(),
                        new ArrayList<>(), chartData, xaxisData));
            }

            // If the widget has optionItems (for select fields), count occurrences in MongoDB
            if (!widget.getOptionItems().isEmpty()) {
                Map<Integer, Integer> optionCounts = countOptionOccurrences(collection, widget.getName(), widget.getOptionItems(), startDateTime, endDateTime);

                // ✅ Instead of modifying `widget.getOptionItems()`, create a NEW WidgetDataDTO and store it in `updatedWidgets`
                List<OptionItemDTO> updatedOptions = widget.getOptionItems().stream()
                        .map(option -> new OptionItemDTO(
                                option.getLabel(),
                                option.getValue(),
                                optionCounts.getOrDefault(option.getValue(), 0)))  // Correctly apply new count values
                        .collect(Collectors.toList());

                updatedWidgets.add(new WidgetDataDTO(widget.getName(), widget.getLabel(), widget.getType(),
                        updatedOptions, null, null));  // Store it correctly
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
            MongoCollection<Document> collection,
            String fieldName,
            List<OptionItemDTO> options,
            Timestamp startDateTime,
            Timestamp endDateTime
    ) {
        Map<Integer, Integer> countMap = new HashMap<>();

        // TODO: what should be the correct counting for this part
        startDateTime = Timestamp.valueOf(startDateTime.toLocalDateTime().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime());
        endDateTime = Timestamp.valueOf(endDateTime.toLocalDateTime().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime());

        List<Document> allDocs = collection.find(exists(fieldName, true)).into(new ArrayList<>());
        List<Document> filteredDocs = new ArrayList<>();

        for (Document doc : allDocs) {
            Date rawDate = doc.getDate("created_at");
            Instant createdAt = rawDate != null ? rawDate.toInstant() : null;
            if (createdAt != null &&
                    !createdAt.isBefore(startDateTime.toInstant()) &&
                    !createdAt.isAfter(endDateTime.toInstant())) {
                filteredDocs.add(doc);
            }
        }

        for (Document doc : filteredDocs) {
            Object val = doc.get(fieldName);
            if (val instanceof List<?>) {
                for (Object item : (List<?>) val) {
                    int intVal = Integer.parseInt(item.toString());
                    countMap.put(intVal, countMap.getOrDefault(intVal, 0) + 1);
                }
            } else if (val instanceof Integer) {
                int intVal = (Integer) val;
                countMap.put(intVal, countMap.getOrDefault(intVal, 0) + 1);
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

        // Get label mappings for formTemplateId
        HashMap<String, Object> optionItemsKeyValueMap = QcFormTemplateOptionItemsKeyValueMapping(formTemplateId);
        HashMap<String, String> keyValueMap = getFormTemplateKeyValueMapping(formTemplateId);

        // Get target collections based on the date range
        List<String> collectionNames = getRelevantCollections(database, formTemplateId, startDateTime, endDateTime);

        Map<String, Document> latestVersionMap = new HashMap<>();
        for (String collectionName : collectionNames) {
            MongoCollection<Document> collection = database.getCollection(collectionName);
            List<Document> collectionRecords = queryRecords(collection, startDateTime, endDateTime, page, size);

            for (Document doc : collectionRecords) {
                String groupId = doc.getString("version_group_id");
                Integer version = doc.getInteger("version", 0);

                if (groupId != null) {
                    Document existing = latestVersionMap.get(groupId);
                    if (existing == null || version > existing.getInteger("version", 0)) {
                        latestVersionMap.put(groupId, doc);
                    }
                } else {
                    // No versioning info, treat as standalone record
                    latestVersionMap.put(doc.getObjectId("_id").toString(), doc);
                }
            }
        }

        // Convert and return only the latest versions
        return latestVersionMap.values().stream()
                .map(doc -> formattedResult(doc, optionItemsKeyValueMap, keyValueMap))
                .collect(Collectors.toList());
    }

    // another function use above fetchQcRecords but filtered by createdBy integer
    @Override
    public List<Document> fetchQcRecordsFilteredByCreator(Long formTemplateId, String startDateTime, String endDateTime, Integer page, Integer size, Integer createdBy) {
        // 先获取全部记录（分页限制会在这里执行）
        List<Document> allRecords = fetchQcRecords(formTemplateId, startDateTime, endDateTime, page, size);

        // 然后再根据 created_by 进行过滤
        return allRecords.stream()
                .filter(doc -> {
                    Object creator = doc.get("created_by");
                    if (creator instanceof Integer) {
                        return creator.equals(createdBy);
                    } else if (creator instanceof Long) {
                        return ((Long) creator).intValue() == createdBy;
                    } else {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Document> fetchAllVersionsByGroupId(Long formTemplateId, String versionGroupId) {
        MongoDatabase database = mongoClient.getDatabase("dev-mes-qc");

        HashMap<String, Object> optionItemsKeyValueMap = QcFormTemplateOptionItemsKeyValueMapping(formTemplateId);
        HashMap<String, String> keyValueMap = getFormTemplateKeyValueMapping(formTemplateId);

        List<Document> versionedDocs = new ArrayList<>();

        // Loop over all relevant collections in here
        for (String collectionName : database.listCollectionNames()) {
            if (!collectionName.startsWith("form_template_" + formTemplateId + "_")) continue;

            MongoCollection<Document> collection = database.getCollection(collectionName);

            List<Document> matches = collection.find(eq("version_group_id", versionGroupId)).into(new ArrayList<>());
            versionedDocs.addAll(matches);
        }

        versionedDocs.sort((a, b) -> {
            Integer v1 = a.getInteger("version", 0);
            Integer v2 = b.getInteger("version", 0);
            return v2.compareTo(v1); // latest first
        });

        return versionedDocs.stream()
                .map(doc -> formattedResult(doc, optionItemsKeyValueMap, keyValueMap))
                .collect(Collectors.toList());
    }

    // TODO: use MongoFormTemplateUtils
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
                extractKeyValuePairs(widgetList, keyValueMap); // Extract key-label mappings
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing form template JSON", e);
        }

        return keyValueMap;
    }

    private void extractKeyValuePairs(List<Document> widgetList, HashMap<String, String> keyValueMap) {
        for (Document widget : widgetList) {
            // Extract options and add name-label pairs to the map
            Document options = (Document) widget.get("options");
            if (options != null) {
                String name = options.getString("name");
                String label = options.getString("label");
                if (name != null && label != null) {
                    keyValueMap.put(name, label);
                }
            }

            // Recursively process nested widgetList
            List<Document> nestedWidgetList = (List<Document>) widget.get("widgetList");
            if (nestedWidgetList != null) {
                extractKeyValuePairs(nestedWidgetList, keyValueMap);
            }

            // Check for widget lists inside grid columns
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

    // TODO: use MongoFormTemplateUtils
    private Document formattedResult(Document document, HashMap<String, Object> optionItemsKeyValueMap, HashMap<String, String> keyValueMap) {
        Document formattedDocument = new Document();

        for (String key : document.keySet()) {
            Object value = document.get(key);

            // 确保 `_id` 仍然是 ObjectId 而不是 timestamp + date 结构
            if ("_id".equals(key) && value instanceof ObjectId) {
                formattedDocument.put("_id", value.toString()); // 转换成字符串
                continue;
            }

            // 替换字段名
            String formattedKey = keyValueMap.getOrDefault(key, key);

            // 如果 value 是选项列表，则转换成对应的 label
            if (optionItemsKeyValueMap.containsKey(key) && value instanceof List) {
                List<?> valueList = (List<?>) value;
                HashMap<String, String> valueToLabelMap = (HashMap<String, String>) optionItemsKeyValueMap.get(key);

                List<String> resolvedLabels = valueList.stream()
                        .map(val -> valueToLabelMap.getOrDefault(val.toString(), val.toString()))
                        .collect(Collectors.toList());
                formattedDocument.put(formattedKey, resolvedLabels);
            }
            // 如果 value 是单个数值并且有 label 映射，则转换
            else if (optionItemsKeyValueMap.containsKey(key) && (value instanceof Integer || value instanceof String)) {
                HashMap<String, String> valueToLabelMap = (HashMap<String, String>) optionItemsKeyValueMap.get(key);
                formattedDocument.put(formattedKey, valueToLabelMap.getOrDefault(value.toString(), value.toString()));
            } else {
                formattedDocument.put(formattedKey, value);
            }

            // 获取 `created_by` 并添加 `created_by_name`
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

        // 🔧 Remap exceeded_info keys from fieldName to fieldLabel
        if (document.containsKey("exceeded_info")) {
            Document originalExceededInfo = (Document) document.get("exceeded_info");
            Document labeledExceededInfo = new Document();

            for (String rawField : originalExceededInfo.keySet()) {
                String labeledField = keyValueMap.getOrDefault(rawField, rawField); // name → label
                labeledExceededInfo.put(labeledField, originalExceededInfo.get(rawField));
            }

            formattedDocument.put("exceeded_info", labeledExceededInfo);
        }

        return formattedDocument;
    }

    // TODO: use MongoFormTemplateUtils
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

    private List<Document> queryRecords(MongoCollection<Document> collection, String startDateTime, String endDateTime, int page, int size) {
        Instant startInstant = convertStringToInstant(startDateTime);
        Instant endInstant = convertStringToInstant(endDateTime);

        System.out.println("Querying records with range: " + startInstant + " to " + endInstant);

        List<Document> allRecords = collection.find().into(new ArrayList<>());
        List<Document> filtered = new ArrayList<>();

        for (Document doc : allRecords) {
            Date rawDate = doc.getDate("created_at");
            Instant createdAt = rawDate != null ? rawDate.toInstant() : null;

            if (createdAt != null && !createdAt.isBefore(startInstant) && !createdAt.isAfter(endInstant)) {
                filtered.add(doc);
            }
        }

        return filtered.stream()
                .skip(page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    private Instant convertStringToInstant(String dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);
        return localDateTime.atZone(ZoneId.of("UTC")).toInstant(); // Force UTC zone
    }

    private Instant convertMongoCreatedAtStringToInstant(String mongoDateTime) {
        try {
            if (mongoDateTime.length() >= 29) { // nanosecond precision
                return Instant.parse(mongoDateTime.substring(0, 26) + "Z"); // truncate to microsecond precision
            } else if (mongoDateTime.length() >= 26) {
                return Instant.parse(mongoDateTime.substring(0, 26) + "Z");
            } else {
                return Instant.parse(mongoDateTime + "Z");
            }
        } catch (Exception e) {
            System.out.println("Error parsing MongoDB created_at: " + mongoDateTime + " - " + e.getMessage());
            return null;
        }
    }


    /**
     * ✅ Converts "yyyy-MM-dd HH:mm:ss" to MongoDB's "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS" format.
     */
    private String convertToMongoDateTimeFormat(String dateTime) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");

        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, inputFormatter);
        return localDateTime.atZone(ZoneId.systemDefault()).format(outputFormatter);
    }

    private String convertToUtcString(String localDateTimeString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZonedDateTime localDateTime = ZonedDateTime.of(LocalDateTime.parse(localDateTimeString, formatter), ZoneId.systemDefault());
        // ZonedDateTime utcDateTime = localDateTime.withZoneSameInstant(ZoneId.of("UTC"));
        // convert to shanghai datetime
        ZonedDateTime shangHaiDateTime = localDateTime.withZoneSameInstant(ZoneId.of("Asia/Shanghai"));
        return shangHaiDateTime.format(formatter);
    }

//    // with time conversion
//    private List<Document> queryRecords(MongoCollection<Document> collection, String startDateTime, String endDateTime, int page, int size) {
//        // ✅ Step 1: Convert "yyyy-MM-dd HH:mm:ss" to `Instant`
//        Instant startInstant = convertStringToInstant(startDateTime);
//        Instant endInstant = convertStringToInstant(endDateTime);
//
//        System.out.println("Querying records with range: " + startInstant + " to " + endInstant);
//
//        // ✅ Step 2: Retrieve all documents from the collection
//        List<Document> allRecords = collection.find().into(new ArrayList<>());
//
//        // ✅ Step 3: Convert `created_at` field from String to `Instant` and add a new field `created_at_time`
//        List<Document> enrichedRecords = new ArrayList<>();
//        for (Document doc : allRecords) {
//            if (doc.containsKey("created_at") && doc.get("created_at") instanceof String) {
//                try {
//                    Instant createdAtInstant = convertMongoCreatedAtStringToInstant(doc.getString("created_at"));
//                    doc.put("created_at_time", createdAtInstant); // Add a new Instant field
//                    enrichedRecords.add(doc);
//                } catch (Exception e) {
//                    System.out.println("Error converting created_at: " + doc.get("created_at") + " - " + e.getMessage());
//                }
//            }
//        }
//
//        // ✅ Step 4: Filter records based on `created_at_time`
//        return enrichedRecords.stream()
//                .filter(doc -> {
//                    Instant createdAt = doc.get("created_at_time", Instant.class);
//                    return createdAt != null && !createdAt.isBefore(startInstant) && !createdAt.isAfter(endInstant);
//                })
//                .skip(page * size) // Apply pagination
//                .limit(size)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * ✅ Converts "yyyy-MM-dd HH:mm:ss" to `Instant`
//     */
//    private Instant convertStringToInstant(String dateTime) {
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);
//
//        // Convert to UTC Instant
//        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
//    }
//
//    /**
//     * ✅ Converts MongoDB's `created_at` format ("2025-01-22T19:34:02.966394038") to `Instant`
//     */
//    private Instant convertMongoCreatedAtStringToInstant(String mongoDateTime) {
//        try {
//            // Handle timestamps with 6 or 9 decimal places dynamically
//            DateTimeFormatter formatter;
//            if (mongoDateTime.length() == 29) { // 9 decimal places (e.g., 2025-01-22T19:34:02.966394038)
//                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");
//            } else if (mongoDateTime.length() == 26) { // 6 decimal places (e.g., 2025-02-28T13:49:31.535097)
//                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
//            } else {
//                throw new IllegalArgumentException("Unexpected created_at format: " + mongoDateTime);
//            }
//
//            return LocalDateTime.parse(mongoDateTime, formatter).atZone(ZoneId.systemDefault()).toInstant();
//        } catch (Exception e) {
//            System.out.println("Error parsing MongoDB created_at: " + mongoDateTime + " - " + e.getMessage());
//            return null;
//        }
//    }
//
//    private String convertToUtcString(String localDateTimeString) {
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        ZonedDateTime localDateTime = ZonedDateTime.of(LocalDateTime.parse(localDateTimeString, formatter), ZoneId.systemDefault());
//        ZonedDateTime utcDateTime = localDateTime.withZoneSameInstant(ZoneId.of("UTC"));
//        // convert to shanghai datetime
////        ZonedDateTime shangHaiDateTime = localDateTime.withZoneSameInstant(ZoneId.of("Asia/Shanghai"));
//        return utcDateTime.format(formatter);
//    }

}
