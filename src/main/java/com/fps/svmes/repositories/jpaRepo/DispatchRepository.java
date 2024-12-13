package com.fps.svmes.repositories.jpaRepo;

import com.fps.svmes.models.sql.Dispatch;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for managing DispatchConfiguration entities.
 */
public interface DispatchRepository extends JpaRepository<Dispatch, Long> {
}
