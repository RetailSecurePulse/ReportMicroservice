package com.retailpulse.dto;

public record InventoryTransactionProductBusinessEntityResponseDto(InventoryTransactionResponseDto inventoryTransaction,
                                                                   ProductResponseDto product,
                                                                   BusinessEntityDto source,
                                                                   BusinessEntityDto destination) {
}
