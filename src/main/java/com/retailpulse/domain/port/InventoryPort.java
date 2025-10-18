package com.retailpulse.domain.port;

import com.retailpulse.dto.InventoryTransactionDto;
import com.retailpulse.dto.InventoryTransactionProductBusinessEntityResponseDto;
import com.retailpulse.dto.ProductResponseDto;
import com.retailpulse.dto.TimeSearchFilterRequestDto;

import java.util.List;

public interface InventoryPort {
    List<InventoryTransactionProductBusinessEntityResponseDto> fetchByDateRange(TimeSearchFilterRequestDto requestDto);
    List<ProductResponseDto> fetchAllProducts();
}
