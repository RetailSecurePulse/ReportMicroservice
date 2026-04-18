package com.retailpulse.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LegacyDtoTest {

    @Test
    void productPricingDto_twoArgumentConstructorCalculatesTotalCost() {
        ProductPricingDto dto = new ProductPricingDto(3, 10.5);

        assertEquals(3, dto.quantity());
        assertEquals(10.5, dto.costPricePerUnit());
        assertEquals(31.5, dto.totalCost());
    }

    @Test
    void productPricingDto_rejectsNegativeQuantity() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProductPricingDto(-1, 10.5, -10.5)
        );

        assertEquals("Quantity cannot be negative", exception.getMessage());
    }

    @Test
    void productPricingDto_rejectsNegativeCostPricePerUnit() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProductPricingDto(1, -10.5, -10.5)
        );

        assertEquals("Cost price per unit cannot be negative", exception.getMessage());
    }

    @Test
    void inventoryTransactionDto_exposesAllFields() {
        ProductResponseDto product = new ProductResponseDto(
                1L, "SKU123", "Sample Product", "Electronics", "Mobile Phones",
                "BrandX", "USA", "Piece", "VEND123", "BAR123456789", 299.99, true
        );
        ProductPricingDto pricing = new ProductPricingDto(2, 19.5);
        BusinessEntityDto source = new BusinessEntityDto("Warehouse A", "Location A", "WAREHOUSE");
        BusinessEntityDto destination = new BusinessEntityDto("Store B", "Location B", "STORE");

        InventoryTransactionDto dto = new InventoryTransactionDto(
                "TXN-001",
                product,
                pricing,
                source,
                destination,
                "2024-01-01T10:00:00Z"
        );

        assertEquals("TXN-001", dto.transactionId());
        assertEquals(product, dto.product());
        assertEquals(pricing, dto.productPricing());
        assertEquals(source, dto.source());
        assertEquals(destination, dto.destination());
        assertEquals("2024-01-01T10:00:00Z", dto.transactionDateTime());
    }

    @Test
    void inventoryTransactionDetailsDto_exposesAllFields() {
        ProductResponseDto product = new ProductResponseDto(
                1L, "SKU123", "Sample Product", "Electronics", "Mobile Phones",
                "BrandX", "USA", "Piece", "VEND123", "BAR123456789", 299.99, true
        );
        InventoryTransactionDto transaction = new InventoryTransactionDto(
                "TXN-001",
                product,
                new ProductPricingDto(1, 9.99),
                new BusinessEntityDto("Warehouse A", "Location A", "WAREHOUSE"),
                new BusinessEntityDto("Store B", "Location B", "STORE"),
                "2024-01-01T10:00:00Z"
        );
        BusinessEntityDto source = new BusinessEntityDto("Warehouse A", "Location A", "WAREHOUSE");
        BusinessEntityDto destination = new BusinessEntityDto("Store B", "Location B", "STORE");

        InventoryTransactionDetailsDto dto =
                new InventoryTransactionDetailsDto(transaction, product, source, destination);

        assertEquals(transaction, dto.inventoryTransaction());
        assertEquals(product, dto.product());
        assertEquals(source, dto.source());
        assertEquals(destination, dto.destination());
    }
}
