package com.fps.svmes.repositories.jpaRepo.formAccessCalendar;

import com.fps.svmes.models.sql.formAccessCalendar.CalendarAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CalendarAssignmentRepository extends JpaRepository<CalendarAssignment, Long> {
    List<CalendarAssignment> findByTeamIdAndStatus(Integer teamId, Integer status);
}