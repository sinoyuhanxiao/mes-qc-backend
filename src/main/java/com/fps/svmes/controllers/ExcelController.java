package com.fps.svmes.controllers;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

@RestController
public class ExcelController {

    @GetMapping("/generate-excel")
    public void generateExcel(HttpServletResponse response) {
        // Set response headers for Excel download
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=quality_control_report.xlsx");

        // Create a new Excel workbook
        try (Workbook workbook = new XSSFWorkbook(); OutputStream outputStream = response.getOutputStream()) {
            Sheet sheet = workbook.createSheet("Quality Control Data");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Inspection Date", "Inspector", "Status", "Remarks"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                CellStyle headerStyle = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);
                cell.setCellStyle(headerStyle);
            }

            // Add 5 rows of fake data
            Object[][] fakeData = {
                    {1, "2025-02-01", "John Doe", "Passed", "No issues found"},
                    {2, "2025-02-02", "Jane Smith", "Failed", "Incorrect assembly"},
                    {3, "2025-02-03", "Mike Johnson", "Passed", "Meets all standards"},
                    {4, "2025-02-04", "Emily Davis", "Pending", "Further review needed"},
                    {5, "2025-02-05", "Chris Brown", "Failed", "Defective part detected"}
            };

            for (int i = 0; i < fakeData.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < fakeData[i].length; j++) {
                    row.createCell(j).setCellValue(fakeData[i][j].toString());
                }
            }

            // Auto-size columns for better readability
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write the workbook to the response output stream
            workbook.write(outputStream);
        } catch (IOException e) {
            throw new RuntimeException("Error generating Excel file", e);
        }
    }
}
