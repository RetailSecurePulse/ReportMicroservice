package com.retailpulse.service.report.extractor;

import com.retailpulse.dto.InventoryTransactionProductBusinessEntityResponseDto;

public class InventoryTransactionDataExtractor implements TableDataExtractor<InventoryTransactionProductBusinessEntityResponseDto> {
    @Override
    public Object[] getRowData(InventoryTransactionProductBusinessEntityResponseDto item, int serialNumber) {
        return new Object[]{
                serialNumber,
                item.inventoryTransaction().id(),
                item.product() != null ? item.product().sku() : "",
                item.product() != null ? item.product().description() : "",
                item.inventoryTransaction() != null ? item.inventoryTransaction().quantity() : "",
                item.source() != null ? item.source().name() : "",
                item.destination() != null ? item.destination().name() : "",
                item.inventoryTransaction().insertedAt()
        };
    }
}