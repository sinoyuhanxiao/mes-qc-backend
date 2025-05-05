package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.alert.AlertRecordDTO;
import com.fps.svmes.dto.dtos.alert.AlertSummaryDTO;
import com.fps.svmes.dto.dtos.alert.DetailedAlertRecordDTO;
import com.fps.svmes.dto.requests.alert.UpdateAlertRecordRequest;
import com.fps.svmes.services.AlertRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/alert-records")
@Slf4j
@RequiredArgsConstructor
public class AlertRecordController {

    private final AlertRecordService alertRecordService;

    /**
     * Updates the RPN value of an alert record (and automatically updates the status based on defined rules).
     * Also records a change log entry for modified fields (e.g., rpn, status).
     *
     * @param request UpdateAlertRecordRequest containing id, rpn, and updatedBy
     * @return the updated AlertRecordDTO
     */
    @PostMapping("/update-record")
    public ResponseEntity<?> updateAlertRecord(@RequestBody UpdateAlertRecordRequest request) {
        try {
            AlertRecordDTO updated = alertRecordService.updateRecord(request.getId(), request.getRpn(), request.getUpdatedBy());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Update failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("update failed：" + e.getMessage());
        }
    }

    /**
     * Deletes an alert record.
     *
     * @param id      The ID of the alert record to delete
     * @param userId  The ID of the user performing the deletion
     * @return the deleted AlertRecordDTO
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAlertRecord(
            @PathVariable Long id,
            @RequestParam("userId") Integer userId
    ) {
        try {
            AlertRecordDTO deleted = alertRecordService.deleteRecord(id, userId);
            return ResponseEntity.ok(deleted);
        } catch (Exception e) {
            log.error("Delete failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("delete failed: " + e.getMessage());
        }
    }

    /**
     * Retrieves a paginated list of detailed alert records.
     *
     * @param page The page number (zero-based)
     * @param size The page size
     * @return A Page of DetailedAlertRecordDTO
     */
    @GetMapping
    public ResponseEntity<?> getAllAlertRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<DetailedAlertRecordDTO> result = alertRecordService.getDetailedList(page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves a summary of alert records.
     *
     * @return An AlertSummaryDTO containing summary information
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getAlertSummary() {
        try {
            AlertSummaryDTO summary = alertRecordService.getAlertSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Failed to get alert summary", e);
            return ResponseEntity.status(500).body("Error fetching summary: " + e.getMessage());
        }
    }



}
