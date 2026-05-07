package com.supermarket.pos.salesapi.model.enums;

/**
 * Represents the lifecycle state of a sale transaction.
 * 
 * State transitions:
 * - ACTIVE → COMPLETED (checkout successful)
 * - ACTIVE → CANCELLED (cashier cancels before checkout)
 * - ACTIVE → FROZEN (cashier pauses the sale)
 * - FROZEN → ACTIVE (cashier resumes the frozen sale)
 * - COMPLETED → RETURNED (full return after checkout)
 * - COMPLETED → PARTIALLY_RETURNED (partial return of specific items)
 */
public enum SaleStatus {
    /**
     * Sale is active and can be modified
     */
    ACTIVE,
    
    /**
     * Sale has been successfully completed
     */
    COMPLETED,
    
    /**
     * Sale has been cancelled before completion
     */
    CANCELLED,
    
    /**
     * Sale is temporarily paused
     */
    FROZEN,
    
    /**
     * All items in the sale have been returned
     */
    RETURNED,
    
    /**
     * Some items in the sale have been returned
     */
    PARTIALLY_RETURNED
}
