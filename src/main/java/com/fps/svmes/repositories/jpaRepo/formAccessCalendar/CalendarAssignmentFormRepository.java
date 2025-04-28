package com.fps.svmes.repositories.jpaRepo.formAccessCalendar;

import com.fps.svmes.models.sql.formAccessCalendar.CalendarAssignmentForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarAssignmentFormRepository extends JpaRepository<CalendarAssignmentForm, Long> {
}