package com.retailpulse.service;

import com.retailpulse.controller.exception.ApplicationException;
import com.retailpulse.domain.ReportDocument;
import com.retailpulse.domain.port.InventoryPort;
import com.retailpulse.dto.*;
import com.retailpulse.infrastructure.ReportDocumentRepository;
import com.retailpulse.service.report.ExcelReportExportService;
import com.retailpulse.service.report.PdfReportExportService;
import com.retailpulse.service.report.extractor.InventoryTransactionDataExtractor;
import com.retailpulse.service.report.extractor.ProductDataExtractor;
import com.retailpulse.service.report.extractor.TableDataExtractor;
import com.retailpulse.util.DateUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class ReportService {
    private final InventoryPort inventoryPort;
    private final ReportDocumentRepository reportDocumentRepository;

    public ReportService(InventoryPort inventoryPort, ReportDocumentRepository reportDocumentRepository) {
        this.inventoryPort = inventoryPort;
        this.reportDocumentRepository = reportDocumentRepository;
    }

    public List<InventoryTransactionProductBusinessEntityResponseDto> findInventoryTransactions(Instant startDateTime, Instant endDateTime) {
        TimeSearchFilterRequestDto requestDto = new TimeSearchFilterRequestDto(startDateTime, endDateTime);
        return inventoryPort.fetchByDateRange(requestDto);
    }

    public List<ProductResponseDto> findAllProducts() {
        return inventoryPort.fetchAllProducts();
    }

    public void exportInventoryTransactionsReport(HttpServletResponse response, Instant start, Instant end, String format) throws IOException {
        List<InventoryTransactionProductBusinessEntityResponseDto> data = findInventoryTransactions(start, end);
        String title = "Inventory Transaction Report";
        String[] headers = {"S/No.", "Transaction ID", "SKU", "Description", "Quantity", "Source", "Destination", "Transaction Date Time"};
        exportReport(response, start, end, format, data, title, headers, new InventoryTransactionDataExtractor());
    }

    public void exportProductReport(HttpServletResponse response, String format) throws IOException {
        List<ProductResponseDto> data = findAllProducts();
        String title = "Product Report";
        String[] headers = {"S/No.", "SKU", "Description", "Category", "Subcategory", "Brand"};
        exportReport(response, null, null, format, data, title, headers, new ProductDataExtractor());
    }

    private <T> void exportReport(HttpServletResponse response, Instant start, Instant end, String format, List<T> data,
                                  String title, String[] headers, TableDataExtractor<T> extractor) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String contentType, extension;

        switch (format.toLowerCase()) {
            case "pdf":
                contentType = "application/pdf";
                extension = ".pdf";
                new PdfReportExportService<>(title, headers, extractor, baos).exportReport(start, end, data);
                break;
            case "excel":
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                extension = ".xlsx";
                new ExcelReportExportService<>(title, headers, extractor, baos).exportReport(start, end, data);
                break;
            default:
                throw new ApplicationException("INVALID_FORMAT", "Unsupported export format: " + format);
        }

        response.setContentType(contentType);
        String fileName = "report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + extension;
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        baos.writeTo(response.getOutputStream());

        reportDocumentRepository.save(new ReportDocument(title, fileName, baos.toByteArray(), start, end));
        baos.close();
        log.info("Report exported successfully: {}", fileName);
    }

    public List<ReportSummaryDto> findAllReports() {
        return reportDocumentRepository.findAllWithoutContent().stream().map(this::toSummary).toList();
    }

    public Content getContent(String id) {
        ReportDocument doc = reportDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + id));

        byte[] bytes = doc.getContent();
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("Report has no content: " + id);
        }

        String fileName = safeFileName(doc.getFileName(), doc.getId());
        MediaType mediaType = MediaTypeFactory.getMediaType(fileName)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return new Content(fileName, mediaType, new ByteArrayResource(bytes), bytes.length, doc.getCreatedAt());
    }

    private ReportSummaryDto toSummary(ReportDocument d) {
        return new ReportSummaryDto(
                d.getId(),
                d.getReportType(),
                d.getFileName(),
                d.getStartDateTime(),
                d.getEndDateTime(),
                d.getCreatedAt()
        );
    }

    private String safeFileName(String fileName, String fallbackId) {
        if (fileName == null || fileName.isBlank()) return "report-" + fallbackId;
        // Strip CR/LF and other dangerous chars
        return fileName.replaceAll("[\\r\\n]", "_");
    }

    // Small holder for controller response composition
    public record Content(
            String fileName,
            MediaType mediaType,
            ByteArrayResource resource,
            long contentLength,
            Instant createdAt
    ) {}

}
