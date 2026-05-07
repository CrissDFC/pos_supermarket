package com.supermarket.pos.salesapi.model.dto;

import java.math.BigDecimal;

/**
 * Summary of a product returned by the Product API.
 */
public record ProductSummary(
    Long id,
    String name,
    String barcode,
    BigDecimal unitPrice,
    Integer availableStock,
    String category
) {}
