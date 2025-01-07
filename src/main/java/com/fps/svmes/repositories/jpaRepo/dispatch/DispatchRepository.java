package com.fps.svmes.repositories.jpaRepo.dispatch;
import java.util.List;
import com.fps.svmes.models.sql.task_schedule.Dispatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for managing DispatchConfiguration entities.
 */
public interface DispatchRepository extends JpaRepository<Dispatch, Long> {
    List<Dispatch> findByStatus(int i);

    @Query("SELECT d FROM Dispatch d LEFT JOIN FETCH d.dispatchForms WHERE d.id = :id")
    Optional<Dispatch> findByIdWithForms(@Param("id") Long id);

    @Query("SELECT d FROM Dispatch d LEFT JOIN FETCH d.dispatchPersonnel WHERE d.id = :id")
    Optional<Dispatch> findByIdWithPersonnel(@Param("id") Long id);
}
