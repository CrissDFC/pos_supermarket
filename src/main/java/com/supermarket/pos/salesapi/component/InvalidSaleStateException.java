package com.supermarket.pos.salesapi.component;

/**
 * Exception thrown when an invalid sale state transition is attempted.
 */
public class InvalidSaleStateException extends RuntimeException {
    
    public InvalidSaleStateException(String message) {
        super(message);
    }
    
    public InvalidSaleStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
