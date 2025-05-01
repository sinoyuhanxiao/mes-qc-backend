package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.formAccessCalendar.CalendarAssignmentDTO;
import com.fps.svmes.models.sql.formAccessCalendar.CalendarAssignment;
import com.fps.svmes.models.sql.formAccessCalendar.CalendarAssignmentForm;
import com.fps.svmes.repositories.jpaRepo.formAccessCalendar.CalendarAssignmentRepository;
import com.fps.svmes.services.CalendarAssignmentService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CalendarAssignmentServiceImpl implements CalendarAssignmentService {

    @Autowired
    private CalendarAssignmentRepository assignmentRepo;

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
        assignment = assignmentRepo.save(assignment);

        // Setup groupId if it's a recurring assignment and groupId was not provided
        if ((assignment.getDaysOfWeek() != null && assignment.getDaysOfWeek().length > 0)) {
            assignment.setGroupId(Math.toIntExact(assignment.getId()));
            assignment = assignmentRepo.save(assignment);
        }

        return modelMapper.map(assignment, CalendarAssignmentDTO.class);
    }

    @Transactional
    @Override
    public CalendarAssignmentDTO updateAssignment(Long id, CalendarAssignmentDTO dto) {
        CalendarAssignment assignment = assignmentRepo.findById(id)
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
        assignment = assignmentRepo.save(assignment);
        return modelMapper.map(assignment, CalendarAssignmentDTO.class);
    }

    @Override
    public void deleteAssignment(Long id, Integer userId) {
        CalendarAssignment assignment = assignmentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        assignment.setUpdateDetails(userId, 0);
        assignmentRepo.save(assignment);
    }

    @Transactional(readOnly = true)
    @Override
    public List<CalendarAssignmentDTO> getAllAssignments() {
        return assignmentRepo.findAll().stream()
                .filter(assignment -> assignment.getStatus() != null && assignment.getStatus() == 1)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public CalendarAssignmentDTO getAssignmentById(Long id) {
        CalendarAssignment assignment = assignmentRepo.findById(id)
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
}
