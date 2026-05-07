package com.supermarket.pos.salesapi.controller;

import com.supermarket.pos.salesapi.model.dto.response.SaleResponse;
import com.supermarket.pos.salesapi.model.entity.CreditNote;
import com.supermarket.pos.salesapi.model.entity.Receipt;
import com.supermarket.pos.salesapi.model.entity.Return;
import com.supermarket.pos.salesapi.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for return operations.
 */
@RestController
@RequestMapping("/api/v1/sales/{saleId}/returns")
@Tag(name = "Returns", description = "Return processing operations")
public class ReturnController {
    
    private final ReturnService returnService;
    
    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }
    
    /**
     * Processes a full return for a completed sale.
     */
    @PostMapping("/full")
    @Operation(summary = "Process a full return")
    public ResponseEntity<ReturnResponse> processFullReturn(
            @PathVariable Long saleId,
            @RequestBody FullReturnRequest request) {
        ReturnService.ReturnResult result = returnService.processFullReturn(saleId, request.reason());
        
        return ResponseEntity.ok(new ReturnResponse(
            result.returnEntity().getId(),
            result.returnEntity().getReturnAmount(),
            result.receipt().getReceiptNumber(),
            result.receipt().getContent(),
            result.creditNote() != null ? result.creditNote().getCreditNoteNumber() : null
        ));
    }
    
    /**
     * Processes a partial return for specific items.
     */
    @PostMapping("/partial")
    @Operation(summary = "Process a partial return")
    public ResponseEntity<ReturnResponse> processPartialReturn(
            @PathVariable Long saleId,
            @RequestBody PartialReturnRequest request) {
        
        List<ReturnService.PartialReturnItem> items = request.items().stream()
            .map(item -> new ReturnService.PartialReturnItem(
                item.saleItemId(),
                item.quantity(),
                item.reason()
            ))
            .toList();
        
        ReturnService.ReturnResult result = returnService.processPartialReturn(saleId, items);
        
        return ResponseEntity.ok(new ReturnResponse(
            result.returnEntity().getId(),
            result.returnEntity().getReturnAmount(),
            result.receipt().getReceiptNumber(),
            result.receipt().getContent(),
            result.creditNote() != null ? result.creditNote().getCreditNoteNumber() : null
        ));
    }
    
    /**
     * Request for full return.
     */
    public record FullReturnRequest(
        @NotBlank(message = "Return reason is required")
        String reason
    ) {}
    
    /**
     * Request for partial return.
     */
    public record PartialReturnRequest(
        @NotNull(message = "Items list is required")
        List<PartialReturnItemRequest> items
    ) {}
    
    /**
     * Item in a partial return request.
     */
    public record PartialReturnItemRequest(
        @NotNull(message = "Sale item ID is required")
        Long saleItemId,
        
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        Integer quantity,
        
        @NotBlank(message = "Return reason is required")
        String reason
    ) {}
    
    /**
     * Response for return operations.
     */
    public record ReturnResponse(
        Long returnId,
        java.math.BigDecimal returnAmount,
        String receiptNumber,
        String receiptContent,
        String creditNoteNumber
    ) {}
}
