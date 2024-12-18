package com.fps.svmes.repositories.jpaRepo.dispatch;
import java.util.List;
import com.fps.svmes.models.sql.task_schedule.Dispatch;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for managing DispatchConfiguration entities.
 */
public interface DispatchRepository extends JpaRepository<Dispatch, Long> {
    List<Dispatch> findByActiveTrue();
}
