package com.retailpulse.infrastructure;

import com.retailpulse.domain.port.InventoryPort;
import com.retailpulse.dto.InventoryTransactionDto;
import com.retailpulse.dto.InventoryTransactionProductBusinessEntityResponseDto;
import com.retailpulse.dto.ProductResponseDto;
import com.retailpulse.dto.TimeSearchFilterRequestDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryFeignAdapter implements InventoryPort {

    private final InventoryFeignClient client;

    public InventoryFeignAdapter(InventoryFeignClient client) {
        this.client = client;
    }

    @Override
    public List<InventoryTransactionProductBusinessEntityResponseDto> fetchByDateRange(TimeSearchFilterRequestDto requestDto) {
        return client.getTransactions(requestDto);
    }

    @Override
    public List<ProductResponseDto> fetchAllProducts() {
        return client.getAllProducts();
    }

}
