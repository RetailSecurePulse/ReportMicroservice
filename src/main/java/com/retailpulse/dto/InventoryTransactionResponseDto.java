package com.retailpulse.dto;

public record InventoryTransactionResponseDto(String id, int quantity, double costPricePerUnit, String insertedAt) {
}
