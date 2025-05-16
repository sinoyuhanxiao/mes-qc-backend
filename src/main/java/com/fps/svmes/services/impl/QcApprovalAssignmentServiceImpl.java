package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.qcForm.QcApprovalAssignmentDTO;
import com.fps.svmes.models.sql.qcForm.QcApprovalAssignment;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcApprovalAssignmentRepository;
import com.fps.svmes.services.QcApprovalAssignmentService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QcApprovalAssignmentServiceImpl implements QcApprovalAssignmentService {

    private final QcApprovalAssignmentRepository repository;
    private final ModelMapper modelMapper;
    private final QcApprovalAssignmentRepository qcApprovalAssignmentRepository;
    private final MongoClient mongoClient;

    @Override
    public void insertIfNotExists(QcApprovalAssignmentDTO dto) {
        Optional<QcApprovalAssignment> existing = repository.findBySubmissionId(dto.getSubmissionId());
        if (existing.isEmpty()) {
            QcApprovalAssignment entity = modelMapper.map(dto, QcApprovalAssignment.class);
            repository.save(entity);
        }
    }

    @Override
    public Page<QcApprovalAssignmentDTO> getAllAssignments(Pageable pageable) {
        return repository.findAll(pageable)
                .map(entity -> modelMapper.map(entity, QcApprovalAssignmentDTO.class));
    }

    @Override
    public Page<QcApprovalAssignmentDTO> getFilteredAssignments(
            String state,
            String approvalType,
            String templateName,
            String startDate,
            String endDate,
            Pageable pageable) {

        Specification<QcApprovalAssignment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(state)) {
                predicates.add(cb.equal(root.get("state"), state));
            }
            if (StringUtils.hasText(approvalType)) {
                predicates.add(cb.equal(root.get("approvalType"), approvalType));
            }
            if (StringUtils.hasText(templateName)) {
                predicates.add(cb.like(root.get("qcFormTemplateName"), "%" + templateName + "%"));
            }
            if (StringUtils.hasText(startDate) && StringUtils.hasText(endDate)) {
                predicates.add(cb.between(root.get("createdAt"),
                        LocalDate.parse(startDate).atStartOfDay(),
                        LocalDate.parse(endDate).atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return qcApprovalAssignmentRepository.findAll(spec, pageable)
                .map(entity -> modelMapper.map(entity, QcApprovalAssignmentDTO.class));
    }

    @Override
    public List<Document> getVersionHistory(String submissionId, String collectionName) {
        // Step 1: Connect to the collection
        MongoDatabase database = mongoClient.getDatabase("dev-mes-qc");
        MongoCollection<Document> collection = database.getCollection(collectionName);

        // Step 2: Find the initial document by submission ID
        Document initial = collection.find(Filters.eq("_id", new org.bson.types.ObjectId(submissionId))).first();
        if (initial == null) throw new IllegalArgumentException("Submission not found");

        // Step 3: Extract version_group_id
        String versionGroupId = initial.getString("version_group_id");

        // Step 4: Return all versions in descending order
        return collection.find(Filters.eq("version_group_id", versionGroupId))
                .sort(new Document("version", -1))  // Descending order
                .into(new ArrayList<>());
    }


}
