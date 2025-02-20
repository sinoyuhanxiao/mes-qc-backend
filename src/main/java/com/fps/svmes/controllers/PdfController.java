package com.fps.svmes.controllers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Map;

@RestController
public class PdfController {

    @PostMapping("/generate-pdf-with-chart")
    public void generatePdfWithChart(@RequestBody Map<String, String> request, HttpServletResponse response) {
        // Extract the Base64 chart image from the request body
        String chartBase64 = request.get("chartImage");

        // Set response headers for PDF download
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=chart_report.pdf");

        // Create the PDF document
        Document document = new Document();

        try (OutputStream outputStream = response.getOutputStream()) {
            // Associate the document with PdfWriter and output stream
            PdfWriter.getInstance(document, outputStream);

            // Open the document
            document.open();

            // Add a heading
            document.add(new Paragraph("ECharts PDF Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.BLUE)));
            document.add(Chunk.NEWLINE); // Add some space

            // Add the chart image to the PDF
            if (chartBase64 != null && !chartBase64.isEmpty()) {
                // Decode Base64 to byte array
                byte[] chartBytes = Base64.getDecoder().decode(chartBase64.split(",")[1]); // Strip data:image prefix
                Image chartImage = Image.getInstance(chartBytes);
                chartImage.scaleToFit(500, 300); // Scale the image to fit the PDF
                chartImage.setAlignment(Element.ALIGN_CENTER);
                document.add(chartImage);
            }

            // Add a paragraph below the chart
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("This is a simple paragraph below the ECharts chart, summarizing the data.", FontFactory.getFont(FontFactory.HELVETICA, 12)));

            // Close the document
            document.close();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Error generating PDF with chart", e);
        }
    }
}
