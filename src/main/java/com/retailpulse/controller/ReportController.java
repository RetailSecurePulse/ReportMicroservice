package com.retailpulse.controller;

import com.retailpulse.controller.exception.ApplicationException;
import com.retailpulse.dto.InventoryTransactionDto;
import com.retailpulse.dto.InventoryTransactionProductBusinessEntityResponseDto;
import com.retailpulse.dto.ReportSummaryDto;
import com.retailpulse.service.ReportService;
import com.retailpulse.util.DateUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
    private static final String INVALID_DATE_RANGE = "INVALID_DATE_RANGE";

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public List<ReportSummaryDto> getAllReports() {
        logger.info("Fetching all reports");
        return reportService.findAllReports();
    }

    /**
     * 2) Return binary content with correct Content-Type.
     *    By default served "inline" so browsers open PDFs/CSV/XLSX/images.
     *    Force download with ?download=true
     */
    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> content(
            @PathVariable("id") String id,
            @RequestParam(name = "download", defaultValue = "false") boolean download
    ) {
        ReportService.Content c = reportService.getContent(id);

        String filename = c.fileName();
        // Encode per RFC 5987 for filename*
        String encoded = UriUtils.encode(filename, StandardCharsets.UTF_8);

        // Build a single Content-Disposition header value
        String dispositionType = download ? "attachment" : "inline";
        // Quote the ASCII filename; sanitize any quotes
        String asciiFilename = filename.replace("\"", "'");
        String contentDisposition = String.format(
                "%s; filename=\"%s\"; filename*=UTF-8''%s",
                dispositionType, asciiFilename, encoded
        );

        return ResponseEntity.ok()
                .contentType(c.mediaType())
                .contentLength(c.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition) // set ONCE
                .body(c.resource());
    }

    @GetMapping("/inventory-transactions")
    public List<InventoryTransactionProductBusinessEntityResponseDto> getInventoryTransactions(@RequestParam("startDateTime") String startDateTime,
                                                                                               @RequestParam("endDateTime") String endDateTime,
                                                                                               @RequestParam("dateTimeFormat") String dateTimeFormat) {
        logger.info("Fetching all inventory transactions");

        if (startDateTime == null || startDateTime.isBlank()) {
            throw new ApplicationException(INVALID_DATE_RANGE, "Start date time parameter cannot be blank");
        }

        if (endDateTime == null || endDateTime.isBlank()) {
            throw new ApplicationException(INVALID_DATE_RANGE, "End date time parameter cannot be blank");
        }

        Instant startInstant;
        Instant endInstant;
        try {
            startInstant = DateUtil.convertStringToInstant(startDateTime, dateTimeFormat);
            endInstant = DateUtil.convertStringToInstant(endDateTime, dateTimeFormat);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new ApplicationException(INVALID_DATE_RANGE, "Invalid date time format");
        }

        if (startInstant.isAfter(endInstant)) {
            throw new ApplicationException(INVALID_DATE_RANGE, "Start date time cannot be after end date time");
        }

        return reportService.findInventoryTransactions(startInstant, endInstant);

    }

    @GetMapping("/inventory-transactions/export")
    public void exportInventoryTransactionReport(HttpServletResponse response,
                                                 @RequestParam("startDateTime") String startDateTime,
                                                 @RequestParam("endDateTime") String endDateTime,
                                                 @RequestParam("dateTimeFormat") String dateTimeFormat,
                                                 @RequestParam("format") String format) throws IOException {
        if (startDateTime == null || startDateTime.isBlank() ||
                endDateTime == null || endDateTime.isBlank()) {
            throw new ApplicationException(INVALID_DATE_RANGE, "Date time parameters cannot be blank");
        }

        Instant start;
        Instant end;
        try {
            start = DateUtil.convertStringToInstant(startDateTime, dateTimeFormat);
            end = DateUtil.convertStringToInstant(endDateTime, dateTimeFormat);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new ApplicationException(INVALID_DATE_RANGE, "Invalid date time format");
        }
        if (start.isAfter(end)) {
            throw new ApplicationException(INVALID_DATE_RANGE, "Start date time cannot be after end date time");
        }
        // Delegate export to the report service using the common template method
        reportService.exportInventoryTransactionsReport(response, start, end, format);
    }

    @GetMapping("/products/export")
    public void exportProductReport(HttpServletResponse response,
                                    @RequestParam("format") String format) throws IOException {
        reportService.exportProductReport(response, format);
    }

}
