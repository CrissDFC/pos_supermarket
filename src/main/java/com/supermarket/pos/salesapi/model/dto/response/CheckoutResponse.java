package com.supermarket.pos.salesapi.model.dto.response;

import com.supermarket.pos.salesapi.model.entity.Receipt;
import com.supermarket.pos.salesapi.model.entity.Sale;

import java.math.BigDecimal;

/**
 * Response DTO for checkout operations.
 */
public record CheckoutResponse(
    SaleResponse sale,
    String receiptNumber,
    String receiptContent,
    BigDecimal change
) {
    public static CheckoutResponse from(Sale sale, Receipt receipt, BigDecimal change) {
        return new CheckoutResponse(
            SaleResponse.from(sale),
            receipt.getReceiptNumber(),
            receipt.getContent(),
            change
        );
    }
}
