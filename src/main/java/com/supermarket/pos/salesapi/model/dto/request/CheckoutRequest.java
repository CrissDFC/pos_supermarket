package com.supermarket.pos.salesapi.model.dto.request;

import com.supermarket.pos.salesapi.model.enums.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request DTO for processing a checkout.
 */
public record CheckoutRequest(
    @NotNull(message = "Payment type is required")
    PaymentType paymentType,
    
    @Positive(message = "Amount received must be positive")
    BigDecimal amountReceived
) {
    /**
     * Validates that amountReceived is provided for CASH payments.
     */
    public void validate() {
        if (paymentType == PaymentType.CASH && amountReceived == null) {
            throw new IllegalArgumentException("Amount received is required for cash payments");
        }
    }
}
