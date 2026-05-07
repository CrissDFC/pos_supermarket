package com.supermarket.pos.salesapi.service;

import com.supermarket.pos.salesapi.client.CustomerApiClient;
import com.supermarket.pos.salesapi.client.ExternalServiceException;
import com.supermarket.pos.salesapi.model.dto.CustomerSummary;
import com.supermarket.pos.salesapi.model.enums.CreditStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for customer-related operations.
 * Delegates to the external Customer API.
 */
@Service
public class CustomerService {
    
    private final CustomerApiClient customerApiClient;
    
    public CustomerService(CustomerApiClient customerApiClient) {
        this.customerApiClient = customerApiClient;
    }
    
    /**
     * Searches for customers by name.
     * 
     * @param name The customer name to search for
     * @return List of matching customers
     * @throws ExternalServiceException if the Customer API is unavailable
     */
    public List<CustomerSummary> searchCustomersByName(String name) {
        return customerApiClient.searchCustomersByName(name);
    }
    
    /**
     * Searches for customers by document number.
     * 
     * @param documentNumber The document number to search for
     * @return List of matching customers
     * @throws ExternalServiceException if the Customer API is unavailable
     */
    public List<CustomerSummary> searchCustomersByDocumentNumber(String documentNumber) {
        return customerApiClient.searchCustomersByDocumentNumber(documentNumber);
    }
    
    /**
     * Gets a customer by ID.
     * 
     * @param customerId The customer ID
     * @return The customer
     * @throws ExternalServiceException if the Customer API is unavailable
     */
    public CustomerSummary getCustomer(Long customerId) {
        return customerApiClient.getCustomer(customerId);
    }
    
    /**
     * Verifies the credit status of a customer.
     * 
     * @param customerId The customer ID
     * @return The credit status
     * @throws ExternalServiceException if the Customer API is unavailable
     */
    public CreditStatus verifyCreditStatus(Long customerId) {
        return customerApiClient.verifyCreditStatus(customerId);
    }
    
    /**
     * Checks if a customer has approved credit status.
     * 
     * @param customerId The customer ID
     * @return true if credit status is APPROVED
     */
    public boolean hasApprovedCredit(Long customerId) {
        CreditStatus status = verifyCreditStatus(customerId);
        return status == CreditStatus.APPROVED;
    }
}
