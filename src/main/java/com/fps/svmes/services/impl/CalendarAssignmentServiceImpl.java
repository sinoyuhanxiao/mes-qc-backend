package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.formAccessCalendar.CalendarAssignmentDTO;
import com.fps.svmes.models.nosql.FormNode;
import com.fps.svmes.models.sql.formAccessCalendar.CalendarAssignment;
import com.fps.svmes.models.sql.formAccessCalendar.CalendarAssignmentForm;
import com.fps.svmes.repositories.jpaRepo.formAccessCalendar.CalendarAssignmentRepository;
import com.fps.svmes.repositories.mongoRepo.FormNodeRepository;
import com.fps.svmes.services.CalendarAssignmentService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalendarAssignmentServiceImpl implements CalendarAssignmentService {

    @Autowired
    private CalendarAssignmentRepository assignmentRepository;

    @Autowired
    private FormNodeRepository formNodeRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    @Override
    public CalendarAssignmentDTO createAssignment(CalendarAssignmentDTO dto) {
        CalendarAssignment assignment = modelMapper.map(dto, CalendarAssignment.class);

        if (dto.getFormTreeNodeIds() != null) {
            CalendarAssignment finalAssignment = assignment;
            List<CalendarAssignmentForm> forms = dto.getFormTreeNodeIds().stream()
                    .map(formId -> {
                        CalendarAssignmentForm form = new CalendarAssignmentForm();
                        form.setAssignment(finalAssignment);
                        form.setFormTreeNodeId(formId);
                        return form;
                    })
                    .collect(Collectors.toList());
            assignment.setForms(forms);
        }

        assignment.setCreationDetails(dto.getCreatedBy(), 1);
        assignment = assignmentRepository.save(assignment);

        // Setup groupId if it's a recurring assignment and groupId was not provided
        if ((assignment.getDaysOfWeek() != null && assignment.getDaysOfWeek().length > 0)) {
            assignment.setGroupId(Math.toIntExact(assignment.getId()));
            assignment = assignmentRepository.save(assignment);
        }

        return modelMapper.map(assignment, CalendarAssignmentDTO.class);
    }

    @Transactional
    @Override
    public CalendarAssignmentDTO updateAssignment(Long id, CalendarAssignmentDTO dto) {
        CalendarAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        modelMapper.map(dto, assignment);

        if (dto.getFormTreeNodeIds() != null) {
            assignment.getForms().clear();
            CalendarAssignment finalAssignment = assignment;
            List<CalendarAssignmentForm> forms = dto.getFormTreeNodeIds().stream()
                    .map(formId -> {
                        CalendarAssignmentForm form = new CalendarAssignmentForm();
                        form.setAssignment(finalAssignment);
                        form.setFormTreeNodeId(formId);
                        return form;
                    })
                    .toList();
            assignment.getForms().addAll(forms);
        }

        assignment.setUpdateDetails(dto.getUpdatedBy(),1);
        assignment = assignmentRepository.save(assignment);
        return modelMapper.map(assignment, CalendarAssignmentDTO.class);
    }

    @Override
    public void deleteAssignment(Long id, Integer userId) {
        CalendarAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        assignment.setUpdateDetails(userId, 0);
        assignmentRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    @Override
    public List<CalendarAssignmentDTO> getAllAssignments() {
        return assignmentRepository.findAll().stream()
                .filter(assignment -> assignment.getStatus() != null && assignment.getStatus() == 1)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public CalendarAssignmentDTO getAssignmentById(Long id) {
        CalendarAssignment assignment = assignmentRepository.findById(id)
                .filter(a -> a.getStatus() != null && a.getStatus() == 1)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));
        return convertToDTO(assignment);
    }

    private CalendarAssignmentDTO convertToDTO(CalendarAssignment calendarAssignment) {
        CalendarAssignmentDTO dto = modelMapper.map(calendarAssignment, CalendarAssignmentDTO.class);

        if (calendarAssignment.getForms() != null){
            List<String> formNodeIds = calendarAssignment.getForms()
                    .stream()
                    .map(CalendarAssignmentForm::getFormTreeNodeId)
                    .collect(Collectors.toList());
            dto.setFormTreeNodeIds(formNodeIds);
        }

        return dto;
    }

    @Transactional
    @Override
    public List<FormNode> getFormTreeByTeamIdAndDate(Integer teamId, OffsetDateTime date) {
        List<CalendarAssignment> assignments = assignmentRepository.findByTeamIdAndStatus(teamId, 1);

        Set<String> matchedFormIds = assignments.stream()
                .filter(a -> isMatchingAssignment(a, date))
                .flatMap(a -> a.getForms().stream().map(CalendarAssignmentForm::getFormTreeNodeId))
                .map(String::trim)
                .collect(Collectors.toSet());

        List<FormNode> fullTree = formNodeRepository.findAll();

        List<FormNode> filteredTree = new ArrayList<>();
        for (FormNode root : fullTree) {
            FormNode filtered = filterTreeByFormIds(root, matchedFormIds);
            if (filtered != null) {
                filteredTree.add(filtered);
            }
        }

        return filteredTree;
    }

    private FormNode filterTreeByFormIds(FormNode node, Set<String> allowedId) {
        if ("document".equalsIgnoreCase(node.getNodeType())) {
            return allowedId.contains(node.getId()) ? node : null;
        }

        List<FormNode> filteredChildren = new ArrayList<>();
        for (FormNode childNode: node.getChildren()) {
            FormNode filteredChild = filterTreeByFormIds(childNode, allowedId);
            if (filteredChild != null) {
                filteredChildren.add(filteredChild);
            }
        }

        if (!filteredChildren.isEmpty()) {
            FormNode newNode = new FormNode();
            newNode.setId(node.getId());
            newNode.setLabel(node.getLabel());
            newNode.setNodeType(node.getNodeType());
            newNode.setQcFormTemplateId(node.getQcFormTemplateId());
            newNode.setChildren(filteredChildren);
            return newNode;
        }

        return null;
    }

    private boolean isMatchingAssignment(CalendarAssignment a, OffsetDateTime dateTime) {
        Integer[] daysArray = a.getDaysOfWeek();

        if (daysArray != null && daysArray.length > 0) {
            // Convert array to Set once for faster lookup
            Set<Integer> daySet = new HashSet<>(Arrays.asList(daysArray));
            int jsDayOfWeek = dateTime.getDayOfWeek().getValue() % 7; // Sunday = 0

            if (!daySet.contains(jsDayOfWeek)) return false;

            boolean afterStart = a.getStartDateRecur() == null || !dateTime.isBefore(a.getStartDateRecur());
            boolean beforeEnd = a.getEndDateRecur() == null || !dateTime.isAfter(a.getEndDateRecur());

            return afterStart && beforeEnd;
        }

        if (a.getStartDate() != null && a.getEndDate() != null) {
            return !dateTime.isBefore(a.getStartDate()) && !dateTime.isAfter(a.getEndDate());
        }

        return false;
    }
}
