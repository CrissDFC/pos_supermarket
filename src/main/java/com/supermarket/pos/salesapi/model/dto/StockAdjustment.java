package com.supermarket.pos.salesapi.model.dto;

/**
 * Represents a stock adjustment for a product.
 */
public record StockAdjustment(
    Long productId,
    Integer quantity
) {}
