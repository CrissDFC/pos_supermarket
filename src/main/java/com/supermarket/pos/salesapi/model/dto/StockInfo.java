package com.supermarket.pos.salesapi.model.dto;

/**
 * Information about product stock.
 */
public record StockInfo(
    Long productId,
    Integer availableQuantity
) {}
