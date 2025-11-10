package com.retailpulse.service;

import com.retailpulse.domain.ReportDocument;
import com.retailpulse.domain.port.InventoryPort;
import com.retailpulse.dto.ProductResponseDto;
import com.retailpulse.infrastructure.ReportDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    private InventoryPort inventoryPort;

    @Mock
    private ReportDocumentRepository reportDocumentRepository;

    @InjectMocks
    private ReportService reportService;

//    @Test
//    void testFindInventoryTransactions_ReturnsExpectedList() {
//        Instant start = Instant.parse("2024-01-01T00:00:00Z");
//        Instant end = Instant.parse("2024-01-02T00:00:00Z");
//        List<InventoryTransactionDto> expected = List.of(
//                new InventoryTransactionDto("T-001",
//                        new ProductResponseDto("SKU-100", "Product 100", "Category A", "Subcategory A", "Brand X"),
//                        new ProductPricingDto(3, 100.5),
//                        new BusinessEntityDto("Store A", "Store 1", "STORE"),
//                        new BusinessEntityDto("Store B", "Store 2", "STORE"),
//                        "2024-01-01T10:00:00Z"
//                ),
//                new InventoryTransactionDto("T-002",
//                        new ProductResponseDto("SKU-200", "Product 200", "Category B", "Subcategory B", "Brand Y"),
//                        new ProductPricingDto(5, 10.5),
//                        new BusinessEntityDto("Store F", "Store 5", "STORE"),
//                        new BusinessEntityDto("Store G", "Store 7", "STORE"),
//                        "2024-02-02T20:00:00Z"
//                )
//        );
//
//        when(inventoryPort.fetchByDateRange(anyString(), anyString())).thenReturn(expected);
//
//        List<InventoryTransactionDto> result = reportService.findInventoryTransactions(start, end);
//
//        assertEquals(expected, result);
//        verify(inventoryPort).fetchByDateRange(anyString(), anyString());
//    }

    @Test
    void testFindAllProducts_ReturnsExpectedList() {
        List<ProductResponseDto> expected = List.of(
                new ProductResponseDto(
                        1L,
                        "SKU123",
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
                ),
                new ProductResponseDto(
                        2L,
                        "SKU234",
                        "Sample Product 2",
                        "Electronics 2",
                        "Mobile Phones 2",
                        "BrandY",
                        "USA",
                        "Piece",
                        "VEND124",
                        "BAR123456780",
                        799.99,
                        true
                )
        );

        when(inventoryPort.fetchAllProducts()).thenReturn(expected);

        List<ProductResponseDto> result = reportService.findAllProducts();

        assertEquals(expected, result);
        verify(inventoryPort).fetchAllProducts();
    }

//    @Test
//    void testExportInventoryTransactionsReport_ExcelFormat_SavesReportAndSetsResponseHeaders() throws Exception {
//        Instant start = Instant.parse("2024-01-01T00:00:00Z");
//        Instant end = Instant.parse("2024-01-02T00:00:00Z");
//        List<InventoryTransactionDto> data = List.of(
//                new InventoryTransactionDto("T-001", null, null, null, null, "2024-01-01T10:00:00Z")
//        );
//        when(inventoryPort.fetchByDateRange(anyString(), anyString())).thenReturn(data);
//
//        // Use Spring's MockHttpServletResponse for testing
//        jakarta.servlet.http.HttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
//
//        reportService.exportInventoryTransactionsReport(response, start, end, "excel");
//
//        // Verify repository save is called
//        verify(reportDocumentRepository).save(any(ReportDocument.class));
//
//        // Optionally, assert response headers/content type
//        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", response.getContentType());
//        assertTrue(response.getHeader("Content-Disposition").contains("attachment; filename="));
//    }

    @Test
    void testExportProductReport_PdfFormat_SavesReportAndSetsResponseHeaders() throws Exception {
        List<ProductResponseDto> data = List.of(
                new ProductResponseDto(
                        1L,
                        "SKU123",
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
                )
        );
        when(inventoryPort.fetchAllProducts()).thenReturn(data);

        jakarta.servlet.http.HttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();

        reportService.exportProductReport(response, "pdf");

        verify(reportDocumentRepository).save(any(ReportDocument.class));
        assertEquals("application/pdf", response.getContentType());
        assertTrue(Objects.requireNonNull(response.getHeader("Content-Disposition")).contains("attachment; filename="));
    }

}
