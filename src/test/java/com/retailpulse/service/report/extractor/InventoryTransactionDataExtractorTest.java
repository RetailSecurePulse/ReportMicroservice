package com.retailpulse.service.report.extractor;

import com.retailpulse.dto.InventoryTransactionProductBusinessEntityResponseDto;
import com.retailpulse.dto.InventoryTransactionResponseDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class InventoryTransactionDataExtractorTest {

    @Test
    void getRowData_usesEmptyStringsForMissingProductAndBusinessEntities() {
        InventoryTransactionDataExtractor extractor = new InventoryTransactionDataExtractor();
        InventoryTransactionProductBusinessEntityResponseDto item =
                new InventoryTransactionProductBusinessEntityResponseDto(
                        new InventoryTransactionResponseDto("TXN-001", 4, 12.50, "2024-01-01T10:00:00Z"),
                        null,
                        null,
                        null
                );

        Object[] rowData = extractor.getRowData(item, 7);

        assertArrayEquals(
                new Object[]{7, "TXN-001", "", "", 4, "", "", "2024-01-01T10:00:00Z"},
                rowData
        );
    }
}
