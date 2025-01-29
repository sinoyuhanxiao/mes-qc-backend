package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.dispatch.QcOrderDTO;
import com.fps.svmes.dto.requests.QcOrderRequest;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.services.QcOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing QC Orders.
 */
@RestController
@RequestMapping("/qc-order")
@Tag(name = "QC Order API", description = "API for QC Orders and related Dispatch operations")
public class QcOrderController {

    @Autowired
    private QcOrderService qcOrderService;

    private static final Logger logger = LoggerFactory.getLogger(QcOrderController.class);

    /**
     * Creates a new QC Order with its Dispatches.
     */
    @Operation(summary = "Create a new QC Order", description = "Creates a QC Order along with its dispatches.")
    @PostMapping("/{userId}")
    public ResponseResult<QcOrderDTO> createQcOrder(@RequestBody @Valid QcOrderRequest request, @PathVariable Integer userId) {
        try {
            QcOrderDTO qcOrderDTO = qcOrderService.createQcOrder(request, userId);
            return ResponseResult.success(qcOrderDTO);
        } catch (Exception e) {
            logger.error("Error creating QC Order", e);
            return ResponseResult.fail("Failed to create QC Order", e);
        }
    }

    /**
     * Updates an existing QC Order and its Dispatches.
     */
    @Operation(summary = "Update an existing QC Order", description = "Updates a QC Order and its associated dispatches.")
    @PutMapping("/{orderId}/{userId}")
    public ResponseResult<QcOrderDTO> updateQcOrder(@PathVariable Long orderId, @RequestBody @Valid QcOrderRequest request, @PathVariable Integer userId) {
        try {
            QcOrderDTO updatedQcOrder = qcOrderService.updateQcOrder(orderId, request, userId);
            return ResponseResult.success(updatedQcOrder);
        } catch (EntityNotFoundException e) {
            logger.error("QC Order not found with ID: {}", orderId, e);
            return ResponseResult.fail("QC Order not found", e);
        } catch (Exception e) {
            logger.error("Error updating QC Order with ID: {}", orderId, e);
            return ResponseResult.fail("Failed to update QC Order", e);
        }
    }

    /**
     * Pauses a Dispatch within a QC Order.
     */
    @Operation(summary = "Pause a Dispatch", description = "Pauses a dispatch within a QC Order and cancels its tasks.")
    @PutMapping("/{orderId}/dispatch/{dispatchId}/pause/{userId}")
    public ResponseResult<String> pauseDispatch(@PathVariable Long orderId, @PathVariable Long dispatchId, @PathVariable Integer userId) {
        try {
            qcOrderService.pauseDispatch(orderId, dispatchId, userId);
            return ResponseResult.success("Dispatch paused successfully.");
        } catch (EntityNotFoundException e) {
            logger.error("Dispatch not found with ID: {}", dispatchId, e);
            return ResponseResult.fail("Dispatch not found", e);
        } catch (Exception e) {
            logger.error("Error pausing dispatch with ID: {}", dispatchId, e);
            return ResponseResult.fail("Failed to pause dispatch", e);
        }
    }

    /**
     * Resumes a paused Dispatch within a QC Order.
     */
    @Operation(summary = "Resume a Dispatch", description = "Resumes a paused dispatch within a QC Order.")
    @PutMapping("/{orderId}/dispatch/{dispatchId}/resume/{userId}")
    public ResponseResult<String> resumeDispatch(@PathVariable Long orderId, @PathVariable Long dispatchId, @PathVariable Integer userId) {
        try {
            qcOrderService.resumeDispatch(orderId, dispatchId, userId);
            return ResponseResult.success("Dispatch resumed successfully.");
        } catch (EntityNotFoundException e) {
            logger.error("Dispatch not found with ID: {}", dispatchId, e);
            return ResponseResult.fail("Dispatch not found", e);
        } catch (Exception e) {
            logger.error("Error resuming dispatch with ID: {}", dispatchId, e);
            return ResponseResult.fail("Failed to resume dispatch", e);
        }
    }

    /**
     * Retrieves a single QC Order by ID.
     */
    @Operation(summary = "Get QC Order by ID", description = "Fetches a QC Order along with its associated dispatches.")
    @GetMapping("/{orderId}")
    public ResponseResult<QcOrderDTO> getQcOrderById(@PathVariable Long orderId) {
        try {
            QcOrderDTO qcOrderDTO = qcOrderService.getQcOrderById(orderId);
            return ResponseResult.success(qcOrderDTO);
        } catch (EntityNotFoundException e) {
            logger.error("QC Order not found with ID: {}", orderId, e);
            return ResponseResult.fail("QC Order not found", e);
        } catch (Exception e) {
            logger.error("Error retrieving QC Order with ID: {}", orderId, e);
            return ResponseResult.fail("Failed to retrieve QC Order", e);
        }
    }

    /**
     * Retrieves a list of all QC Orders.
     */
    @Operation(summary = "Get all QC Orders", description = "Retrieves all QC Orders with their associated dispatches.")
    @GetMapping
    public ResponseResult<List<QcOrderDTO>> getAllQcOrders() {
        try {
            List<QcOrderDTO> qcOrders = qcOrderService.getAllQcOrders();
            return qcOrders.isEmpty()
                    ? ResponseResult.noContent(qcOrders)
                    : ResponseResult.success(qcOrders);
        } catch (Exception e) {
            logger.error("Error retrieving all QC Orders", e);
            return ResponseResult.fail("Failed to retrieve QC Orders", e);
        }
    }

    /**
     * Deletes a QC Order by ID.
     */
    @Operation(summary = "Delete a QC Order", description = "Soft deletes a QC Order and its associated dispatches.")
    @DeleteMapping("/{orderId}")
    public ResponseResult<String> deleteQcOrder(@PathVariable Long orderId) {
        try {
            qcOrderService.deleteQcOrder(orderId);
            return ResponseResult.success("QC Order deleted successfully.");
        } catch (EntityNotFoundException e) {
            logger.error("QC Order not found with ID: {}", orderId, e);
            return ResponseResult.fail("QC Order not found", e);
        } catch (Exception e) {
            logger.error("Error deleting QC Order with ID: {}", orderId, e);
            return ResponseResult.fail("Failed to delete QC Order", e);
        }
    }
}
