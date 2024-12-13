package com.fps.svmes.models.sql;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Represents a test dispatched to a specific personnel.
 */
@Entity
@Data
public class DispatchedTest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Dispatch dispatch; // Associated configuration

    private Long personnelId; // ID of the personnel receiving the test

    private Long formId; // ID of the dispatched form

    private LocalDateTime dispatchTime; // Time when the test was dispatched

    private String status; // Status of the test (e.g., "PENDING", "COMPLETED")
}
