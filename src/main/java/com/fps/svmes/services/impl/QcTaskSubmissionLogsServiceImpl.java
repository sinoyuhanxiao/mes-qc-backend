package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.qcForm.QcTaskSubmissionLogsDTO;
import com.fps.svmes.models.sql.qcForm.QcTaskSubmissionLogs;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcFormTemplateRepository;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcTaskSubmissionLogsRepository;
import com.fps.svmes.services.QcTaskSubmissionLogsService;
import com.fps.svmes.services.UserService;
import com.itextpdf.text.Paragraph;

import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.query.Query;
import com.itextpdf.text.*;

import java.time.*;
import java.util.Map;

import org.bson.Document;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.fps.svmes.controllers.UserController.logger;


@Service
public class QcTaskSubmissionLogsServiceImpl implements QcTaskSubmissionLogsService {
    @Autowired
    private QcTaskSubmissionLogsRepository repository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private QcFormTemplateRepository qcFormTemplateRepository;

    @Autowired
    private UserService userService;

    @Override
    public QcTaskSubmissionLogsDTO insertLog(QcTaskSubmissionLogsDTO dto) {
        // Map the DTO to the entity
        QcTaskSubmissionLogs log = modelMapper.map(dto, QcTaskSubmissionLogs.class);

        // Set timestamps
        log.setCreatedAt(OffsetDateTime.now());

        // Save the entity
        QcTaskSubmissionLogs savedLog = repository.save(log);

        // Map the saved entity back to DTO
        return modelMapper.map(savedLog, QcTaskSubmissionLogsDTO.class);
    }

    @Override
    public List<QcTaskSubmissionLogsDTO> getAllByCreatedByAndTaskId(Integer createdBy, Long dispatchedTaskId) {
        List<QcTaskSubmissionLogs> logs = repository.findAllByCreatedByAndDispatchedTaskId(createdBy, dispatchedTaskId);
        return logs.stream()
                .map(log -> modelMapper.map(log, QcTaskSubmissionLogsDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public Document getDocumentBySubmissionId(String submissionId, Long formId, Integer createdBy) {
        try {
            // Log input parameters for debugging
            logger.info("Fetching document with submissionId: {}, formId: {}, createdBy: {}", submissionId, formId, createdBy);

            // Validate submissionId
            if (!ObjectId.isValid(submissionId)) {
                logger.error("Invalid submissionId format: {}", submissionId);
                throw new IllegalArgumentException("Invalid submissionId format");
            }

            // Generate the collection name dynamically
            String yearMonth = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String collectionName = "form_template_" + formId + "_" + yearMonth;
            logger.info("Looking in collection: {}", collectionName);

            // Check if collection exists
            if (!mongoTemplate.collectionExists(collectionName)) {
                logger.error("Collection does not exist: {}", collectionName);
                throw new RuntimeException("Collection not found: " + collectionName);
            }

            // Construct the MongoDB query
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").is(new ObjectId(submissionId)));

            logger.info("Constructed query: {}", query);

            // Execute the query and fetch the document
            Document document = mongoTemplate.findOne(query, Document.class, collectionName);

            if (document == null) {
                logger.warn("No document found for submissionId: {}", submissionId);
                return null;
            }

            logger.info("Document retrieved successfully: {}", document);
            return formattedResult(document, formId);

        } catch (Exception e) {
            logger.error("Error retrieving document with submissionId: {}, formId: {}, createdBy: {}", submissionId, formId, createdBy, e);
            throw new RuntimeException("Error retrieving document from MongoDB: " + e.getMessage(), e);
        }
    }

    public HashMap<String, String> getFormTemplateKeyValueMapping(Long formId) {
        String formTemplateJson = qcFormTemplateRepository.findFormTemplateJsonById(formId); // get only that single result for now

        if (formTemplateJson == null || formTemplateJson.isEmpty()) {
            throw new RuntimeException("Form template JSON not found for formId: " + formId);
        }

        HashMap<String, String> keyValueMap = new HashMap<>();

        try {
            Document formTemplate = Document.parse(formTemplateJson);
            List<Document> widgetList = (List<Document>) formTemplate.get("widgetList");

            if (widgetList != null) {
                extractKeyValuePairs(widgetList, keyValueMap); // Start recursive extraction
            }
        } catch (Exception e) {
            logger.error("Error parsing form template JSON for formId: {}", formId, e);
            throw new RuntimeException("Error parsing form template JSON", e);
        }

        return keyValueMap;
    }

    private HashMap<String, Object> QcFormTemplateOptionItemsKeyValueMapping(Long formId) {
        String formTemplateJson = qcFormTemplateRepository.findFormTemplateJsonById(formId);

        if (formTemplateJson == null || formTemplateJson.isEmpty()) {
            throw new RuntimeException("Form template JSON not found for formId: " + formId);
        }

        HashMap<String, Object> optionItemsKeyValueMap = new HashMap<>();

        try {
            Document formTemplate = Document.parse(formTemplateJson);
            List<Document> widgetList = (List<Document>) formTemplate.get("widgetList");

            if (widgetList != null) {
                extractOptionItems(widgetList, optionItemsKeyValueMap); // Start recursive extraction
            }
        } catch (Exception e) {
            logger.error("Error parsing form template JSON for formId: {}", formId, e);
            throw new RuntimeException("Error parsing form template JSON", e);
        }

        return optionItemsKeyValueMap;
    }

    private void extractOptionItems(List<Document> widgetList, HashMap<String, Object> optionItemsKeyValueMap) {
        for (Document widget : widgetList) {
            // Extract options with optionItems
            Document options = (Document) widget.get("options");
            if (options != null) {
                String label = options.getString("label");
                List<Document> optionItems = (List<Document>) options.get("optionItems");
                if (label != null && optionItems != null) {
                    HashMap<String, String> valueToLabelMap = new HashMap<>();
                    for (Document option : optionItems) {
                        Object value = option.get("value");
                        String optionLabel = option.getString("label");
                        if (value != null && optionLabel != null) {
                            valueToLabelMap.put(value.toString(), optionLabel);
                        }
                    }
                    optionItemsKeyValueMap.put(label, valueToLabelMap);
                }
            }

            // Recursively process nested widgetList
            List<Document> nestedWidgetList = (List<Document>) widget.get("widgetList");
            if (nestedWidgetList != null) {
                extractOptionItems(nestedWidgetList, optionItemsKeyValueMap);
            }

            // Check for widget lists inside grid columns
            List<Document> cols = (List<Document>) widget.get("cols");
            if (cols != null) {
                for (Document col : cols) {
                    List<Document> colWidgetList = (List<Document>) col.get("widgetList");
                    if (colWidgetList != null) {
                        extractOptionItems(colWidgetList, optionItemsKeyValueMap);
                    }
                }
            }
        }
    }

    private void extractKeyValuePairs(List<Document> widgetList, HashMap<String, String> keyValueMap) {
        for (Document widget : widgetList) {
            // Extract options and add name-label pairs to the map
            Document options = (Document) widget.get("options");
            if (options != null) {
                String name = options.getString("name");
                String label = options.getString("label");
                if (name != null && label != null) {
                    keyValueMap.put(name, label);
                }
            }

            // Recursively process nested widgetList
            List<Document> nestedWidgetList = (List<Document>) widget.get("widgetList");
            if (nestedWidgetList != null) {
                extractKeyValuePairs(nestedWidgetList, keyValueMap);
            }

            // Check for widget lists inside grid columns
            List<Document> cols = (List<Document>) widget.get("cols");
            if (cols != null) {
                for (Document col : cols) {
                    List<Document> colWidgetList = (List<Document>) col.get("widgetList");
                    if (colWidgetList != null) {
                        extractKeyValuePairs(colWidgetList, keyValueMap);
                    }
                }
            }
        }
    }


    public Document formattedResult(Document document, Long formId) {
        HashMap<String, String> keyValueMap = getFormTemplateKeyValueMapping(formId);
        HashMap<String, Object> optionItemsKeyValueMap = QcFormTemplateOptionItemsKeyValueMapping(formId);

        Document formattedDocument = new Document();
        for (String key : document.keySet()) {
            Object value = document.get(key);

            // Replace key with its corresponding label or retain original key
            String formattedKey = keyValueMap.getOrDefault(key, key);

            // If the value is a list and matches an optionItems mapping
            if (optionItemsKeyValueMap.containsKey(formattedKey) && value instanceof List) {
                List<?> valueList = (List<?>) value;
                HashMap<String, String> valueToLabelMap = (HashMap<String, String>) optionItemsKeyValueMap.get(formattedKey);

                List<String> resolvedLabels = valueList.stream()
                        .map(val -> valueToLabelMap.getOrDefault(val.toString(), val.toString()))
                        .collect(Collectors.toList());
                formattedDocument.put(formattedKey, resolvedLabels);
            }
            // Single value with a matching optionItems mapping
            else if (optionItemsKeyValueMap.containsKey(formattedKey) && (value instanceof String || value instanceof Integer)) {
                HashMap<String, String> valueToLabelMap = (HashMap<String, String>) optionItemsKeyValueMap.get(formattedKey);
                formattedDocument.put(formattedKey, valueToLabelMap.getOrDefault(value.toString(), value.toString()));
            } else {
                formattedDocument.put(formattedKey, value);
            }
        }
        return formattedDocument;
    }

    @Override
    public List<Document> getDocumentsByQcFormTemplateIdAndCreatedBy(Long qcFormTemplateId, Integer createdBy) {
        try {
            // Generate the collection name dynamically
            String yearMonth = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String collectionName = "form_template_" + qcFormTemplateId + "_" + yearMonth;

            // Check if collection exists
            if (!mongoTemplate.collectionExists(collectionName)) {
                throw new RuntimeException("Collection not found: " + collectionName);
            }

            // Construct the MongoDB query
            Query query = new Query();
            query.addCriteria(Criteria.where("created_by").is(createdBy));

            // Execute the query and fetch the documents
            List<Document> documents = mongoTemplate.find(query, Document.class, collectionName);

            // Format each document
            List<Document> formattedDocuments = documents.stream()
                    .map(document -> formattedResult(document, qcFormTemplateId))
                    .collect(Collectors.toList());

            return formattedDocuments;
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving documents from MongoDB: " + e.getMessage(), e);
        }
    }

    // the downloaded name should be the filename
    @Override
    public byte[] exportDocumentsToExcel(List<Document> documents) {
        try {
            // Create a new workbook
            Workbook workbook = new XSSFWorkbook();
            // name it to current datetime
            String fileName = "documents_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
            Sheet sheet = workbook.createSheet(fileName);

            // Create header row
            Row headerRow = sheet.createRow(0);
            int cellNum = 0;
            for (String key : documents.get(0).keySet()) {
                Cell cell = headerRow.createCell(cellNum++);
                cell.setCellValue(key);
            }

            // Populate data rows
            int rowNum = 1;
            for (Document document : documents) {
                Row row = sheet.createRow(rowNum++);
                cellNum = 0;
                for (Object value : document.values()) {
                    Cell cell = row.createCell(cellNum++);
                    if (value instanceof List) {
                        cell.setCellValue(String.join(", ", (List<String>) value));
                    } else {
                        cell.setCellValue(value.toString());
                    }
                }
            }

            // Write workbook to ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error exporting documents to Excel: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportDocumentToPdf(Document mongoDocument) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // Create a new PDF document (iText)
            com.itextpdf.text.Document pdfDocument = new com.itextpdf.text.Document();
            PdfWriter.getInstance(pdfDocument, outputStream);

            // Open the PDF document
            pdfDocument.open();

            // Load the SimSun font
            String fontPath = "C:/Windows/Fonts/simsun.ttc";
            BaseFont baseFont = BaseFont.createFont(fontPath + ",0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font chineseFont = new Font(baseFont, 12, Font.NORMAL);

            // Add the title "提交记录"
            pdfDocument.add(new Paragraph("提交记录", new Font(baseFont, 18, Font.BOLD, BaseColor.BLUE)));
            pdfDocument.add(Chunk.NEWLINE); // Add a blank line for spacing

            // Create a table to dynamically display the key-value pairs
            PdfPTable table = new PdfPTable(2); // Two columns: one for the key and one for the value
            table.setWidthPercentage(100); // Table spans the full width
            table.setSpacingBefore(10f);
            table.setWidths(new float[]{2f, 5f}); // Adjust column widths

            // Dynamically iterate over the MongoDB document and handle specific keys
            for (Map.Entry<String, Object> entry : mongoDocument.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // Custom key mapping
                if ("_id".equals(key)) {
                    key = "提交单号"; // Change "_id" to "表单ID"
                } else if ("created_at".equals(key)) {
                    key = "提交时间"; // Change "created_at" to "提交时间"
                    // Convert "created_at" value to local timezone in "YYYY-MM-DD HH:mm:ss" format
                    value = convertToLocalTime(value.toString());
                } else if ("created_by".equals(key)) {
                    key = "提交人ID"; // Change "created_by" to "提交人ID"
                }

                // Add key and value to the table
                PdfPCell keyCell = new PdfPCell(new Phrase(key, chineseFont));
                keyCell.setBorder(Rectangle.NO_BORDER);
                keyCell.setPadding(5);

                PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value.toString() : "", chineseFont));
                valueCell.setBorder(Rectangle.NO_BORDER);
                valueCell.setPadding(5);

                table.addCell(keyCell);
                table.addCell(valueCell);
            }

            // Add the table to the PDF document
            pdfDocument.add(table);

            // Add the closing "提交时间" at the end
            pdfDocument.add(Chunk.NEWLINE);
            String createdAt = mongoDocument.getString("created_at"); // Retrieve "created_at" value dynamically
            pdfDocument.add(new Paragraph(
                    "提交时间: " + (createdAt != null ? convertToLocalTime(createdAt) : "N/A"),
                    chineseFont
            ));
            // find the person name using the userservice
            String personName = userService.getUserById(
                            Integer.parseInt(String.valueOf(mongoDocument.get("created_by"))))
                    .getName();
            pdfDocument.add(new Paragraph(
                    "提交人: " + personName,
                    chineseFont
            ));

            // Close the PDF document
            pdfDocument.close();

            // Return the generated PDF as a byte array
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error exporting document to PDF: " + e.getMessage(), e);
        }
    }

    private String convertToLocalTime(String utcTime) {
        try {
            // Parse the ISO-8601 UTC timestamp into an Instant
            utcTime = utcTime + 'Z';
            Instant instant = Instant.parse(utcTime);

            // Convert the Instant to the system's default timezone
            ZonedDateTime localZoned = instant.atZone(ZoneId.systemDefault());

            // Format the time as "YYYY-MM-DD HH:mm:ss"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return localZoned.format(formatter);

        } catch (Exception e) {
            // Fallback: return the original UTC time if parsing fails
            return utcTime;
        }
    }

}
