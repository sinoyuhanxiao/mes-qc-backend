// Controller
package com.fps.svmes.controllers;

import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;
import com.fps.svmes.dto.responses.ResponseResult;
import com.fps.svmes.services.QcFormTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/qc-form-templates")
@RequiredArgsConstructor
@Tag(name = "QC Form Templates", description = "API for QC Form Templates")
public class QcFormTemplateController {

    private final QcFormTemplateService service;
    private static final Logger logger = LoggerFactory.getLogger(QcFormTemplateController.class);

    @GetMapping
    @Operation(summary = "Get all active QC form templates", description = "Returns a list of all active QC form templates.")
    public ResponseResult<List<QcFormTemplateDTO>> getAllActiveTemplates() {
        try {
            List<QcFormTemplateDTO> templates = service.getAllActiveTemplates();
            logger.info("Retrieved {} active templates", templates.size());
            return ResponseResult.success(templates);
        } catch (Exception e) {
            logger.error("Error retrieving active templates", e);
            return ResponseResult.fail("Error retrieving active templates", e);
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a QC form template by ID", description = "Fetches the QC form template by its ID.")
    public ResponseResult<QcFormTemplateDTO> getTemplateById(@PathVariable Long id) {
        try {
            QcFormTemplateDTO template = service.getTemplateById(id);
            logger.info("Template retrieved with ID: {}", id);
            return ResponseResult.success(template);
        } catch (Exception e) {
            logger.error("Error retrieving template with ID: {}", id, e);
            return ResponseResult.fail("Error retrieving template", e);
        }
    }

    @PostMapping
    @Operation(summary = "Create a new QC form template", description = "Creates a new QC form template.")
    public ResponseResult<QcFormTemplateDTO> createTemplate(@RequestBody QcFormTemplateDTO dto) {
        try {
            QcFormTemplateDTO createdTemplate = service.createTemplate(dto);
            logger.info("Template created: {}", createdTemplate);
            return ResponseResult.success(createdTemplate);
        } catch (Exception e) {
            logger.error("Error creating template", e);
            return ResponseResult.fail("Error creating template", e);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a QC form template", description = "Updates an existing QC form template.")
    public ResponseResult<QcFormTemplateDTO> updateTemplate(@PathVariable Long id, @RequestBody QcFormTemplateDTO dto) {
        try {
            QcFormTemplateDTO updatedTemplate = service.updateTemplate(id, dto);
            logger.info("Template updated with ID: {}", id);
            return ResponseResult.success(updatedTemplate);
        } catch (Exception e) {
            logger.error("Error updating template with ID: {}", id, e);
            return ResponseResult.fail("Error updating template", e);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a QC form template", description = "Marks a QC form template as inactive by setting its status to 0.")
    public ResponseResult<Void> deleteTemplate(@PathVariable Long id) {
        try {
            service.deleteTemplate(id);
            logger.info("Template deleted with ID: {}", id);
            return ResponseResult.success(null);
        } catch (Exception e) {
            logger.error("Error deleting template with ID: {}", id, e);
            return ResponseResult.fail("Error deleting template", e);
        }
    }
}
