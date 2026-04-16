package com.retailpulse.controller;

import com.retailpulse.controller.exception.ApplicationException;
import com.retailpulse.dto.InventoryTransactionProductBusinessEntityResponseDto;
import com.retailpulse.dto.ReportSummaryDto;
import com.retailpulse.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @Test
    void getAllReports_returnsSummariesFromService() {
        List<ReportSummaryDto> expected = List.of(
                new ReportSummaryDto(
                        "report-1",
                        "Product Report",
                        "report.pdf",
                        Instant.parse("2024-01-01T00:00:00Z"),
                        Instant.parse("2024-01-02T00:00:00Z"),
                        Instant.parse("2024-01-03T00:00:00Z")
                )
        );

        when(reportService.findAllReports()).thenReturn(expected);

        List<ReportSummaryDto> result = reportController.getAllReports();

        assertEquals(expected, result);
        verify(reportService).findAllReports();
    }

    @Test
    void content_usesInlineDispositionByDefault() {
        ByteArrayResource resource = new ByteArrayResource("hello".getBytes());
        ReportService.Content content = new ReportService.Content(
                "monthly report.pdf",
                MediaType.APPLICATION_PDF,
                resource,
                5,
                Instant.parse("2024-01-03T00:00:00Z")
        );

        when(reportService.getContent("report-1")).thenReturn(content);

        ResponseEntity<org.springframework.core.io.Resource> response = reportController.content("report-1", false);

        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertEquals(5, response.getHeaders().getContentLength());
        assertEquals(
                "inline; filename=\"monthly report.pdf\"; filename*=UTF-8''monthly%20report.pdf",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
        );
        assertSame(resource, response.getBody());
    }

    @Test
    void content_withDownloadTrue_usesAttachmentDispositionAndSanitizesQuotes() {
        ByteArrayResource resource = new ByteArrayResource("hello".getBytes());
        ReportService.Content content = new ReportService.Content(
                "monthly \"report\".pdf",
                MediaType.APPLICATION_PDF,
                resource,
                5,
                Instant.parse("2024-01-03T00:00:00Z")
        );

        when(reportService.getContent("report-1")).thenReturn(content);

        ResponseEntity<org.springframework.core.io.Resource> response = reportController.content("report-1", true);

        assertEquals(
                "attachment; filename=\"monthly 'report'.pdf\"; filename*=UTF-8''monthly%20%22report%22.pdf",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
        );
    }

    @Test
    void getInventoryTransactions_withBlankStart_throwsApplicationException() {
        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> reportController.getInventoryTransactions("", "2024-01-02 00:00:00", "yyyy-MM-dd HH:mm:ss")
        );

        assertEquals("INVALID_DATE_RANGE", exception.getErrorCode());
        assertEquals("Start date time parameter cannot be blank", exception.getMessage());
    }

    @Test
    void getInventoryTransactions_withInvalidDateFormat_throwsApplicationException() {
        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> reportController.getInventoryTransactions(
                        "2024/01/01 00:00:00",
                        "2024/01/02 00:00:00",
                        "yyyy-MM-dd HH:mm:ss"
                )
        );

        assertEquals("INVALID_DATE_RANGE", exception.getErrorCode());
        assertEquals("Invalid date time format", exception.getMessage());
    }

    @Test
    void getInventoryTransactions_withStartAfterEnd_throwsApplicationException() {
        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> reportController.getInventoryTransactions(
                        "2024-01-03 00:00:00",
                        "2024-01-02 00:00:00",
                        "yyyy-MM-dd HH:mm:ss"
                )
        );

        assertEquals("INVALID_DATE_RANGE", exception.getErrorCode());
        assertEquals("Start date time cannot be after end date time", exception.getMessage());
    }

    @Test
    void getInventoryTransactions_withValidInputs_callsServiceWithParsedInstants() {
        List<InventoryTransactionProductBusinessEntityResponseDto> expected = List.of();
        String format = "yyyy-MM-dd HH:mm:ss";

        when(reportService.findInventoryTransactions(any(Instant.class), any(Instant.class))).thenReturn(expected);

        List<InventoryTransactionProductBusinessEntityResponseDto> result = reportController.getInventoryTransactions(
                "2024-01-01 00:00:00",
                "2024-01-02 12:30:00",
                format
        );

        ArgumentCaptor<Instant> startCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> endCaptor = ArgumentCaptor.forClass(Instant.class);

        assertSame(expected, result);
        verify(reportService).findInventoryTransactions(startCaptor.capture(), endCaptor.capture());
        assertEquals(Instant.parse("2023-12-31T16:00:00Z"), startCaptor.getValue());
        assertEquals(Instant.parse("2024-01-02T04:30:00Z"), endCaptor.getValue());
    }

    @Test
    void exportInventoryTransactionReport_withBlankDate_throwsApplicationException() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> reportController.exportInventoryTransactionReport(
                        response,
                        "",
                        "2024-01-02 00:00:00",
                        "yyyy-MM-dd HH:mm:ss",
                        "pdf"
                )
        );

        assertEquals("INVALID_DATE_RANGE", exception.getErrorCode());
        assertEquals("Date time parameters cannot be blank", exception.getMessage());
    }

    @Test
    void exportInventoryTransactionReport_withValidInputs_delegatesToService() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        reportController.exportInventoryTransactionReport(
                response,
                "2024-01-01 00:00:00",
                "2024-01-02 12:30:00",
                "yyyy-MM-dd HH:mm:ss",
                "excel"
        );

        verify(reportService).exportInventoryTransactionsReport(
                eq(response),
                eq(Instant.parse("2023-12-31T16:00:00Z")),
                eq(Instant.parse("2024-01-02T04:30:00Z")),
                eq("excel")
        );
    }

    @Test
    void exportProductReport_delegatesToService() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        reportController.exportProductReport(response, "pdf");

        verify(reportService).exportProductReport(response, "pdf");
    }
}
