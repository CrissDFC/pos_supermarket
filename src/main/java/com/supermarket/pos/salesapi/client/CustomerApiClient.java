package com.supermarket.pos.salesapi.client;

import com.supermarket.pos.salesapi.model.dto.CustomerSummary;
import com.supermarket.pos.salesapi.model.enums.CreditStatus;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Client for communicating with the external Customer API.
 * 
 * Uses Resilience4j for fault tolerance:
 * - Circuit breaker: Opens after 50% failure rate
 * - Retry: 3 attempts with 100ms wait
 * - Time limiter: 10 second timeout
 */
@Component
public class CustomerApiClient {
    
    private final RestTemplate restTemplate;
    private final String customerApiBaseUrl;
    
    public CustomerApiClient(RestTemplate restTemplate,
                            @Value("${external-api.customer.base-url}") String customerApiBaseUrl) {
        this.restTemplate = restTemplate;
        this.customerApiBaseUrl = customerApiBaseUrl;
    }
    
    /**
     * Searches for customers by name (partial match).
     * 
     * @param name The customer name to search for
     * @return List of matching customers
     */
    @CircuitBreaker(name = "customerApi", fallbackMethod = "searchCustomersFallback")
    @Retry(name = "customerApi")
    public List<CustomerSummary> searchCustomersByName(String name) {
        String url = customerApiBaseUrl + "/api/v1/customers/search?name=" + name;
        CustomerSummary[] customers = restTemplate.getForObject(url, CustomerSummary[].class);
        return customers != null ? Arrays.asList(customers) : Collections.emptyList();
    }
    
    /**
     * Searches for a customer by document number (exact match).
     * 
     * @param documentNumber The document number to search for
     * @return List containing the matching customer (empty if not found)
     */
    @CircuitBreaker(name = "customerApi", fallbackMethod = "searchCustomersFallback")
    @Retry(name = "customerApi")
    public List<CustomerSummary> searchCustomersByDocumentNumber(String documentNumber) {
        String url = customerApiBaseUrl + "/api/v1/customers/search?documentNumber=" + documentNumber;
        CustomerSummary[] customers = restTemplate.getForObject(url, CustomerSummary[].class);
        return customers != null ? Arrays.asList(customers) : Collections.emptyList();
    }
    
    /**
     * Gets a customer by ID.
     * 
     * @param customerId The customer ID
     * @return The customer if found
     */
    @CircuitBreaker(name = "customerApi", fallbackMethod = "getCustomerFallback")
    @Retry(name = "customerApi")
    public CustomerSummary getCustomer(Long customerId) {
        String url = customerApiBaseUrl + "/api/v1/customers/" + customerId;
        return restTemplate.getForObject(url, CustomerSummary.class);
    }
    
    /**
     * Verifies the credit status of a customer.
     * 
     * @param customerId The customer ID
     * @return The credit status
     */
    @CircuitBreaker(name = "customerApi", fallbackMethod = "verifyCreditStatusFallback")
    @Retry(name = "customerApi")
    public CreditStatus verifyCreditStatus(Long customerId) {
        String url = customerApiBaseUrl + "/api/v1/customers/" + customerId + "/credit-status";
        return restTemplate.getForObject(url, CreditStatus.class);
    }
    
    // Fallback methods
    
    private List<CustomerSummary> searchCustomersFallback(String query, Throwable t) {
        throw new ExternalServiceException("Customer API is temporarily unavailable", "CustomerApi", t);
    }
    
    private CustomerSummary getCustomerFallback(Long customerId, Throwable t) {
        throw new ExternalServiceException("Customer API is temporarily unavailable", "CustomerApi", t);
    }
    
    private CreditStatus verifyCreditStatusFallback(Long customerId, Throwable t) {
        throw new ExternalServiceException("Customer API is temporarily unavailable", "CustomerApi", t);
    }
}
