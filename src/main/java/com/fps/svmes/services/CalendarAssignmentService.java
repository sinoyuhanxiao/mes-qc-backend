package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.formAccessCalendar.CalendarAssignmentDTO;
import java.util.List;

public interface CalendarAssignmentService {
    CalendarAssignmentDTO createAssignment(CalendarAssignmentDTO assignmentRequestDTO);
    CalendarAssignmentDTO updateAssignment(Long id, CalendarAssignmentDTO dto);
    void deleteAssignment(Long id, Integer userId);
    List<CalendarAssignmentDTO> getAllAssignments();
    CalendarAssignmentDTO getAssignmentById(Long id);
}