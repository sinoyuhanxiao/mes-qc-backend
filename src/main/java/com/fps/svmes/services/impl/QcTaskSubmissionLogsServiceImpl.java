package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.qcForm.QcTaskSubmissionLogsDTO;
import com.fps.svmes.models.sql.qcForm.QcTaskSubmissionLogs;
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
    public Document getDocumentBySubmissionId(String submissionId, String formId, Integer createdBy) {
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
            return document;

        } catch (Exception e) {
            logger.error("Error retrieving document with submissionId: {}, formId: {}, createdBy: {}", submissionId, formId, createdBy, e);
            throw new RuntimeException("Error retrieving document from MongoDB: " + e.getMessage(), e);
        }
    }


}
