package com.retailpulse.service;

import com.retailpulse.controller.exception.ApplicationException;
import com.retailpulse.domain.ReportDocument;
import com.retailpulse.domain.port.InventoryPort;
import com.retailpulse.dto.*;
import com.retailpulse.infrastructure.ReportDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private InventoryPort inventoryPort;

    @Mock
    private ReportDocumentRepository reportDocumentRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void findInventoryTransactions_returnsExpectedListAndPassesRequestDto() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-02T00:00:00Z");
        List<InventoryTransactionProductBusinessEntityResponseDto> expected = List.of(sampleTransaction());

        when(inventoryPort.fetchByDateRange(any(TimeSearchFilterRequestDto.class))).thenReturn(expected);

        List<InventoryTransactionProductBusinessEntityResponseDto> result =
                reportService.findInventoryTransactions(start, end);

        ArgumentCaptor<TimeSearchFilterRequestDto> requestCaptor =
                ArgumentCaptor.forClass(TimeSearchFilterRequestDto.class);

        assertEquals(expected, result);
        verify(inventoryPort).fetchByDateRange(requestCaptor.capture());
        assertEquals(start, requestCaptor.getValue().startDateTime());
        assertEquals(end, requestCaptor.getValue().endDateTime());
    }

    @Test
    void findAllProducts_returnsExpectedList() {
        List<ProductResponseDto> expected = List.of(sampleProduct(1L, "SKU123"));

        when(inventoryPort.fetchAllProducts()).thenReturn(expected);

        List<ProductResponseDto> result = reportService.findAllProducts();

        assertEquals(expected, result);
        verify(inventoryPort).fetchAllProducts();
    }

    @Test
    void exportInventoryTransactionsReport_excel_savesReportAndSetsResponseHeaders() throws IOException {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-02T00:00:00Z");
        List<InventoryTransactionProductBusinessEntityResponseDto> data = List.of(sampleTransaction());
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(inventoryPort.fetchByDateRange(any(TimeSearchFilterRequestDto.class))).thenReturn(data);

        reportService.exportInventoryTransactionsReport(response, start, end, "excel");

        ArgumentCaptor<ReportDocument> documentCaptor = ArgumentCaptor.forClass(ReportDocument.class);

        verify(reportDocumentRepository).save(documentCaptor.capture());
        assertEquals(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                response.getContentType()
        );
        assertTrue(Objects.requireNonNull(response.getHeader("Content-Disposition")).contains(".xlsx"));
        assertTrue(response.getContentAsByteArray().length > 0);
        assertEquals("Inventory Transaction Report", documentCaptor.getValue().getReportType());
        assertEquals(start, documentCaptor.getValue().getStartDateTime());
        assertEquals(end, documentCaptor.getValue().getEndDateTime());
        assertTrue(documentCaptor.getValue().getFileName().endsWith(".xlsx"));
        assertTrue(documentCaptor.getValue().getContent().length > 0);
    }

    @Test
    void exportProductReport_pdf_savesReportAndSetsResponseHeaders() throws IOException {
        List<ProductResponseDto> data = List.of(sampleProduct(1L, "SKU123"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(inventoryPort.fetchAllProducts()).thenReturn(data);

        reportService.exportProductReport(response, "pdf");

        ArgumentCaptor<ReportDocument> documentCaptor = ArgumentCaptor.forClass(ReportDocument.class);

        verify(reportDocumentRepository).save(documentCaptor.capture());
        assertEquals("application/pdf", response.getContentType());
        assertTrue(Objects.requireNonNull(response.getHeader("Content-Disposition")).contains(".pdf"));
        assertTrue(response.getContentAsByteArray().length > 0);
        assertEquals("Product Report", documentCaptor.getValue().getReportType());
        assertTrue(documentCaptor.getValue().getFileName().endsWith(".pdf"));
        assertTrue(documentCaptor.getValue().getContent().length > 0);
    }

    @Test
    void exportProductReport_withUnsupportedFormat_throwsApplicationException() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> reportService.exportProductReport(response, "csv")
        );

        assertEquals("INVALID_FORMAT", exception.getErrorCode());
        assertEquals("Unsupported export format: csv", exception.getMessage());
        verify(reportDocumentRepository, never()).save(any(ReportDocument.class));
    }

    @Test
    void findAllReports_returnsMappedSummaries() {
        Instant createdAt = Instant.parse("2024-01-03T12:00:00Z");
        ReportDocument document = new ReportDocument(
                "Product Report",
                "report_20240103_120000.pdf",
                new byte[]{1, 2, 3},
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-02T00:00:00Z")
        );
        document.setId("report-1");
        document.setCreatedAt(createdAt);

        when(reportDocumentRepository.findAllWithoutContent()).thenReturn(List.of(document));

        List<ReportSummaryDto> result = reportService.findAllReports();

        assertEquals(1, result.size());
        assertEquals("report-1", result.getFirst().id());
        assertEquals("Product Report", result.getFirst().reportType());
        assertEquals("report_20240103_120000.pdf", result.getFirst().fileName());
        assertEquals(createdAt, result.getFirst().createdAt());
    }

    @Test
    void getContent_returnsSanitizedFileMetadataAndBytes() {
        Instant createdAt = Instant.parse("2024-01-03T12:00:00Z");
        byte[] bytes = "pdf-content".getBytes();
        ReportDocument document = new ReportDocument();
        document.setId("report-1");
        document.setFileName("report\r\n.pdf");
        document.setCreatedAt(createdAt);
        document.setContent(bytes);

        when(reportDocumentRepository.findById("report-1")).thenReturn(Optional.of(document));

        ReportService.Content content = reportService.getContent("report-1");

        assertEquals("report__.pdf", content.fileName());
        assertEquals("application/pdf", content.mediaType().toString());
        assertEquals(bytes.length, content.contentLength());
        assertEquals(createdAt, content.createdAt());
        assertNotNull(content.resource());
        assertArrayEquals(bytes, content.resource().getByteArray());
    }

    @Test
    void getContent_withoutBytes_throwsIllegalStateException() {
        ReportDocument document = new ReportDocument();
        document.setId("report-2");
        document.setFileName("report.pdf");
        document.setContent(new byte[0]);

        when(reportDocumentRepository.findById("report-2")).thenReturn(Optional.of(document));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> reportService.getContent("report-2")
        );

        assertEquals("Report has no content: report-2", exception.getMessage());
    }

    @Test
    void getContent_withNullBytes_throwsIllegalStateException() {
        ReportDocument document = new ReportDocument();
        document.setId("report-3");
        document.setFileName("report.pdf");
        document.setContent(null);

        when(reportDocumentRepository.findById("report-3")).thenReturn(Optional.of(document));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> reportService.getContent("report-3")
        );

        assertEquals("Report has no content: report-3", exception.getMessage());
    }

    @Test
    void getContent_withNullFileName_usesFallbackFileName() {
        byte[] bytes = "content".getBytes();
        ReportDocument document = new ReportDocument();
        document.setId("report-4");
        document.setFileName(null);
        document.setContent(bytes);

        when(reportDocumentRepository.findById("report-4")).thenReturn(Optional.of(document));

        ReportService.Content content = reportService.getContent("report-4");

        assertEquals("report-report-4", content.fileName());
        assertEquals("application/octet-stream", content.mediaType().toString());
        assertEquals(bytes.length, content.contentLength());
    }

    @Test
    void getContent_withBlankFileName_usesFallbackFileName() {
        byte[] bytes = "content".getBytes();
        ReportDocument document = new ReportDocument();
        document.setId("report-5");
        document.setFileName("  ");
        document.setContent(bytes);

        when(reportDocumentRepository.findById("report-5")).thenReturn(Optional.of(document));

        ReportService.Content content = reportService.getContent("report-5");

        assertEquals("report-report-5", content.fileName());
        assertEquals(bytes.length, content.contentLength());
    }

    private InventoryTransactionProductBusinessEntityResponseDto sampleTransaction() {
        return new InventoryTransactionProductBusinessEntityResponseDto(
                new InventoryTransactionResponseDto("TXN-001", 3, 99.99, "2024-01-01T10:00:00Z"),
                sampleProduct(1L, "SKU123"),
                new BusinessEntityDto("Warehouse A", "Location A", "WAREHOUSE"),
                new BusinessEntityDto("Store B", "Location B", "STORE")
        );
    }

    private ProductResponseDto sampleProduct(Long id, String sku) {
        return new ProductResponseDto(
                id,
                sku,
                "Sample Product",
                "Electronics",
                "Mobile Phones",
                "BrandX",
                "USA",
                "Piece",
                "VEND123",
                "BAR123456789",
                299.99,
                true
        );
    }
}
