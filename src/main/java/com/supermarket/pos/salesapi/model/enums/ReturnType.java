package com.supermarket.pos.salesapi.model.enums;

/**
 * Represents the type of return transaction.
 */
public enum ReturnType {
    /**
     * All items in the sale are returned
     */
    FULL,
    
    /**
     * Only specific items and quantities are returned
     */
    PARTIAL
}
