package com.supermarket.pos.salesapi.model.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating a new sale.
 */
public record CreateSaleRequest(
    @NotBlank(message = "Terminal ID is required")
    String terminalId,
    
    @NotBlank(message = "Cashier ID is required")
    String cashierId,
    
    Long customerId
) {}
