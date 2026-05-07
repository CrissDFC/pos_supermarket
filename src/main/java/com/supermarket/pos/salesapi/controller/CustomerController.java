package com.supermarket.pos.salesapi.controller;

import com.supermarket.pos.salesapi.model.dto.CustomerSummary;
import com.supermarket.pos.salesapi.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for customer search operations.
 */
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Customer search operations")
public class CustomerController {
    
    private final CustomerService customerService;
    
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }
    
    /**
     * Searches for customers by name or document number.
     */
    @GetMapping("/search")
    @Operation(summary = "Search customers by name or document number")
    public ResponseEntity<List<CustomerSummary>> searchCustomers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String documentNumber) {
        
        List<CustomerSummary> customers;
        
        if (documentNumber != null && !documentNumber.trim().isEmpty()) {
            customers = customerService.searchCustomersByDocumentNumber(documentNumber);
        } else if (name != null && !name.trim().isEmpty()) {
            customers = customerService.searchCustomersByName(name);
        } else {
            customers = List.of();
        }
        
        return ResponseEntity.ok(customers);
    }
}
