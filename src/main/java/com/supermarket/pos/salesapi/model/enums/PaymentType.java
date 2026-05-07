package com.supermarket.pos.salesapi.model.enums;

/**
 * Represents the payment method for a sale transaction.
 */
public enum PaymentType {
    /**
     * Cash payment - customer association is optional
     */
    CASH,
    
    /**
     * Credit payment - customer association is mandatory
     */
    CREDIT
}
