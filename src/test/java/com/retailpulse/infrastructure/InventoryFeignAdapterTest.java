package com.retailpulse.infrastructure;

import com.retailpulse.dto.InventoryTransactionProductBusinessEntityResponseDto;
import com.retailpulse.dto.ProductResponseDto;
import com.retailpulse.dto.TimeSearchFilterRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryFeignAdapterTest {

    @Mock
    private InventoryFeignClient client;

    @InjectMocks
    private InventoryFeignAdapter adapter;

    @Test
    void fetchByDateRange_delegatesToClient() {
        TimeSearchFilterRequestDto request =
                new TimeSearchFilterRequestDto(Instant.parse("2024-01-01T00:00:00Z"), Instant.parse("2024-01-02T00:00:00Z"));
        List<InventoryTransactionProductBusinessEntityResponseDto> expected = List.of();

        when(client.getTransactions(request)).thenReturn(expected);

        List<InventoryTransactionProductBusinessEntityResponseDto> result = adapter.fetchByDateRange(request);

        assertEquals(expected, result);
        verify(client).getTransactions(request);
    }

    @Test
    void fetchAllProducts_delegatesToClient() {
        List<ProductResponseDto> expected = List.of(
                new ProductResponseDto(1L, "SKU123", "Sample Product", "Electronics", "Mobile Phones",
                        "BrandX", "USA", "Piece", "VEND123", "BAR123456789", 299.99, true)
        );

        when(client.getAllProducts()).thenReturn(expected);

        List<ProductResponseDto> result = adapter.fetchAllProducts();

        assertEquals(expected, result);
        verify(client).getAllProducts();
    }
}
