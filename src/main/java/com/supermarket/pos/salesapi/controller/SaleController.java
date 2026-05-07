package com.supermarket.pos.salesapi.controller;

import com.supermarket.pos.salesapi.model.dto.request.AddItemRequest;
import com.supermarket.pos.salesapi.model.dto.request.CancelSaleRequest;
import com.supermarket.pos.salesapi.model.dto.request.CheckoutRequest;
import com.supermarket.pos.salesapi.model.dto.request.CreateSaleRequest;
import com.supermarket.pos.salesapi.model.dto.response.CheckoutResponse;
import com.supermarket.pos.salesapi.model.dto.response.SaleResponse;
import com.supermarket.pos.salesapi.model.entity.Sale;
import com.supermarket.pos.salesapi.model.enums.PaymentType;
import com.supermarket.pos.salesapi.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for sale operations.
 */
@RestController
@RequestMapping("/api/v1/sales")
@Tag(name = "Sales", description = "Sale management operations")
public class SaleController {
    
    private final SaleService saleService;
    private final FreezeService freezeService;
    private final PaymentService paymentService;
    
    public SaleController(SaleService saleService,
                         FreezeService freezeService,
                         PaymentService paymentService) {
        this.saleService = saleService;
        this.freezeService = freezeService;
        this.paymentService = paymentService;
    }
    
    /**
     * Creates a new sale.
     */
    @PostMapping
    @Operation(summary = "Create a new sale")
    public ResponseEntity<SaleResponse> createSale(@Valid @RequestBody CreateSaleRequest request) {
        Sale sale = saleService.createSale(
            request.terminalId(),
            request.cashierId(),
            request.customerId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(SaleResponse.from(sale));
    }
    
    /**
     * Gets a sale by ID.
     */
    @GetMapping("/{saleId}")
    @Operation(summary = "Get a sale by ID")
    public ResponseEntity<SaleResponse> getSale(@PathVariable Long saleId) {
        Sale sale = saleService.getSale(saleId);
        return ResponseEntity.ok(SaleResponse.from(sale));
    }
    
    /**
     * Adds an item to a sale.
     */
    @PostMapping("/{saleId}/items")
    @Operation(summary = "Add an item to a sale")
    public ResponseEntity<SaleResponse> addItem(
            @PathVariable Long saleId,
            @Valid @RequestBody AddItemRequest request) {
        Sale sale = saleService.addItem(saleId, request.productId(), request.quantity());
        return ResponseEntity.ok(SaleResponse.from(sale));
    }
    
    /**
     * Updates the quantity of an item.
     */
    @PutMapping("/{saleId}/items/{itemId}")
    @Operation(summary = "Update item quantity")
    public ResponseEntity<SaleResponse> updateItemQuantity(
            @PathVariable Long saleId,
            @PathVariable Long itemId,
            @RequestParam Integer quantity) {
        Sale sale = saleService.updateItemQuantity(saleId, itemId, quantity);
        return ResponseEntity.ok(SaleResponse.from(sale));
    }
    
    /**
     * Removes an item from a sale.
     */
    @DeleteMapping("/{saleId}/items/{itemId}")
    @Operation(summary = "Remove an item from a sale")
    public ResponseEntity<SaleResponse> removeItem(
            @PathVariable Long saleId,
            @PathVariable Long itemId) {
        Sale sale = saleService.removeItem(saleId, itemId);
        return ResponseEntity.ok(SaleResponse.from(sale));
    }
    
    /**
     * Associates a customer with a sale.
     */
    @PutMapping("/{saleId}/customer")
    @Operation(summary = "Associate a customer with a sale")
    public ResponseEntity<SaleResponse> associateCustomer(
            @PathVariable Long saleId,
            @RequestParam Long customerId) {
        Sale sale = saleService.associateCustomer(saleId, customerId);
        return ResponseEntity.ok(SaleResponse.from(sale));
    }
    
    /**
     * Cancels a sale.
     */
    @PostMapping("/{saleId}/cancel")
    @Operation(summary = "Cancel a sale")
    public ResponseEntity<SaleResponse> cancelSale(
            @PathVariable Long saleId,
            @Valid @RequestBody CancelSaleRequest request) {
        Sale sale = saleService.cancelSale(saleId, request.reason());
        return ResponseEntity.ok(SaleResponse.from(sale));
    }
    
    /**
     * Freezes a sale.
     */
    @PostMapping("/{saleId}/freeze")
    @Operation(summary = "Freeze a sale")
    public ResponseEntity<SaleResponse> freezeSale(@PathVariable Long saleId) {
        Sale sale = freezeService.freezeSale(saleId);
        return ResponseEntity.ok(SaleResponse.from(sale));
    }
    
    /**
     * Resumes a frozen sale.
     */
    @PostMapping("/{saleId}/resume")
    @Operation(summary = "Resume a frozen sale")
    public ResponseEntity<SaleResponse> resumeSale(@PathVariable Long saleId) {
        Sale sale = freezeService.resumeSale(saleId);
        return ResponseEntity.ok(SaleResponse.from(sale));
    }
    
    /**
     * Gets frozen sales by terminal.
     */
    @GetMapping("/frozen")
    @Operation(summary = "Get frozen sales by terminal")
    public ResponseEntity<List<SaleResponse>> getFrozenSales(@RequestParam String terminalId) {
        List<Sale> sales = freezeService.getFrozenSalesByTerminal(terminalId);
        List<SaleResponse> responses = sales.stream()
            .map(SaleResponse::from)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Processes a checkout.
     */
    @PostMapping("/{saleId}/checkout")
    @Operation(summary = "Process checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @PathVariable Long saleId,
            @Valid @RequestBody CheckoutRequest request) {
        request.validate();
        
        PaymentService.CheckoutResult result;
        if (request.paymentType() == PaymentType.CASH) {
            result = paymentService.processCashCheckout(saleId, request.amountReceived());
        } else {
            result = paymentService.processCreditCheckout(saleId);
        }
        
        return ResponseEntity.ok(CheckoutResponse.from(
            result.sale(),
            result.receipt(),
            result.change()
        ));
    }
}
