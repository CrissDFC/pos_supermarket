package com.supermarket.pos.salesapi.client;

/**
 * Exception thrown when an external service is unavailable.
 */
public class ExternalServiceException extends RuntimeException {
    
    private final String serviceName;
    
    public ExternalServiceException(String message, String serviceName) {
        super(message);
        this.serviceName = serviceName;
    }
    
    public ExternalServiceException(String message, String serviceName, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
    }
    
    public String getServiceName() {
        return serviceName;
    }
}
