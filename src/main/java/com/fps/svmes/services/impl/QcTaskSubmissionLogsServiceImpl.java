package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.qcForm.QcTaskSubmissionLogsDTO;
import com.fps.svmes.models.sql.qcForm.QcTaskSubmissionLogs;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcFormTemplateRepository;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcTaskSubmissionLogsRepository;
import com.fps.svmes.services.QcTaskSubmissionLogsService;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.query.Query;

import org.bson.Document;
import org.bson.types.ObjectId;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.fps.svmes.controllers.UserController.logger;


@Service
public class QcTaskSubmissionLogsServiceImpl implements QcTaskSubmissionLogsService {
    @Autowired
    private QcTaskSubmissionLogsRepository repository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private QcFormTemplateRepository qcFormTemplateRepository;

    @Override
    public QcTaskSubmissionLogsDTO insertLog(QcTaskSubmissionLogsDTO dto) {
        // Map the DTO to the entity
        QcTaskSubmissionLogs log = modelMapper.map(dto, QcTaskSubmissionLogs.class);

        // Set timestamps
        log.setCreatedAt(OffsetDateTime.now());

        // Save the entity
        QcTaskSubmissionLogs savedLog = repository.save(log);

        // Map the saved entity back to DTO
        return modelMapper.map(savedLog, QcTaskSubmissionLogsDTO.class);
    }

    @Override
    public List<QcTaskSubmissionLogsDTO> getAllByCreatedByAndTaskId(Integer createdBy, Long dispatchedTaskId) {
        List<QcTaskSubmissionLogs> logs = repository.findAllByCreatedByAndDispatchedTaskId(createdBy, dispatchedTaskId);
        return logs.stream()
                .map(log -> modelMapper.map(log, QcTaskSubmissionLogsDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public Document getDocumentBySubmissionId(String submissionId, Long formId, Integer createdBy) {
        try {
            // Log input parameters for debugging
            logger.info("Fetching document with submissionId: {}, formId: {}, createdBy: {}", submissionId, formId, createdBy);

            // Validate submissionId
            if (!ObjectId.isValid(submissionId)) {
                logger.error("Invalid submissionId format: {}", submissionId);
                throw new IllegalArgumentException("Invalid submissionId format");
            }

            // Generate the collection name dynamically
            String yearMonth = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String collectionName = "form_template_" + formId + "_" + yearMonth;
            logger.info("Looking in collection: {}", collectionName);

            // Check if collection exists
            if (!mongoTemplate.collectionExists(collectionName)) {
                logger.error("Collection does not exist: {}", collectionName);
                throw new RuntimeException("Collection not found: " + collectionName);
            }

            // Construct the MongoDB query
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").is(new ObjectId(submissionId)));

            logger.info("Constructed query: {}", query);

            // Execute the query and fetch the document
            Document document = mongoTemplate.findOne(query, Document.class, collectionName);

            if (document == null) {
                logger.warn("No document found for submissionId: {}", submissionId);
                return null;
            }

            logger.info("Document retrieved successfully: {}", document);
            return formattedResult(document, formId);

        } catch (Exception e) {
            logger.error("Error retrieving document with submissionId: {}, formId: {}, createdBy: {}", submissionId, formId, createdBy, e);
            throw new RuntimeException("Error retrieving document from MongoDB: " + e.getMessage(), e);
        }
    }

    public HashMap<String, String> getFormTemplateKeyValueMapping(Long formId) {
        String formTemplateJson = qcFormTemplateRepository.findFormTemplateJsonById(formId); // get only that single result for now

        if (formTemplateJson == null || formTemplateJson.isEmpty()) {
            throw new RuntimeException("Form template JSON not found for formId: " + formId);
        }

        HashMap<String, String> keyValueMap = new HashMap<>();

        try {
            Document formTemplate = Document.parse(formTemplateJson);
            List<Document> widgetList = (List<Document>) formTemplate.get("widgetList");

            if (widgetList != null) {
                extractKeyValuePairs(widgetList, keyValueMap); // Start recursive extraction
            }
        } catch (Exception e) {
            logger.error("Error parsing form template JSON for formId: {}", formId, e);
            throw new RuntimeException("Error parsing form template JSON", e);
        }

        return keyValueMap;
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
                extractOptionItems(widgetList, optionItemsKeyValueMap); // Start recursive extraction
            }
        } catch (Exception e) {
            logger.error("Error parsing form template JSON for formId: {}", formId, e);
            throw new RuntimeException("Error parsing form template JSON", e);
        }

        return optionItemsKeyValueMap;
    }

    private void extractOptionItems(List<Document> widgetList, HashMap<String, Object> optionItemsKeyValueMap) {
        for (Document widget : widgetList) {
            // Extract options with optionItems
            Document options = (Document) widget.get("options");
            if (options != null) {
                String label = options.getString("label");
                List<Document> optionItems = (List<Document>) options.get("optionItems");
                if (label != null && optionItems != null) {
                    HashMap<String, String> valueToLabelMap = new HashMap<>();
                    for (Document option : optionItems) {
                        Object value = option.get("value");
                        String optionLabel = option.getString("label");
                        if (value != null && optionLabel != null) {
                            valueToLabelMap.put(value.toString(), optionLabel);
                        }
                    }
                    optionItemsKeyValueMap.put(label, valueToLabelMap);
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


    public Document formattedResult(Document document, Long formId) {
        HashMap<String, String> keyValueMap = getFormTemplateKeyValueMapping(formId);
        HashMap<String, Object> optionItemsKeyValueMap = QcFormTemplateOptionItemsKeyValueMapping(formId);

        Document formattedDocument = new Document();
        for (String key : document.keySet()) {
            Object value = document.get(key);

            // Replace key with its corresponding label or retain original key
            String formattedKey = keyValueMap.getOrDefault(key, key);

            // If the value is a list and matches an optionItems mapping
            if (optionItemsKeyValueMap.containsKey(formattedKey) && value instanceof List) {
                List<?> valueList = (List<?>) value;
                HashMap<String, String> valueToLabelMap = (HashMap<String, String>) optionItemsKeyValueMap.get(formattedKey);

                List<String> resolvedLabels = valueList.stream()
                        .map(val -> valueToLabelMap.getOrDefault(val.toString(), val.toString()))
                        .collect(Collectors.toList());
                formattedDocument.put(formattedKey, resolvedLabels);
            }
            // Single value with a matching optionItems mapping
            else if (optionItemsKeyValueMap.containsKey(formattedKey) && (value instanceof String || value instanceof Integer)) {
                HashMap<String, String> valueToLabelMap = (HashMap<String, String>) optionItemsKeyValueMap.get(formattedKey);
                formattedDocument.put(formattedKey, valueToLabelMap.getOrDefault(value.toString(), value.toString()));
            } else {
                formattedDocument.put(formattedKey, value);
            }
        }
        return formattedDocument;
    }

}
