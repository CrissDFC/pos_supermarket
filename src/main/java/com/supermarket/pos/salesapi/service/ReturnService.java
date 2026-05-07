package com.supermarket.pos.salesapi.service;

import com.supermarket.pos.salesapi.client.ExternalServiceException;
import com.supermarket.pos.salesapi.component.InvalidSaleStateException;
import com.supermarket.pos.salesapi.component.ReceiptGenerator;
import com.supermarket.pos.salesapi.component.SaleStateMachine;
import com.supermarket.pos.salesapi.exception.ResourceNotFoundException;
import com.supermarket.pos.salesapi.model.dto.StockAdjustment;
import com.supermarket.pos.salesapi.model.entity.*;
import com.supermarket.pos.salesapi.model.enums.PaymentType;
import com.supermarket.pos.salesapi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for processing returns.
 */
@Service
@Transactional
public class ReturnService {
    
    private final SaleRepository saleRepository;
    private final ReturnRepository returnRepository;
    private final ReturnItemRepository returnItemRepository;
    private final ReceiptRepository receiptRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final ProductService productService;
    private final SaleStateMachine stateMachine;
    private final ReceiptGenerator receiptGenerator;
    
    public ReturnService(SaleRepository saleRepository,
                        ReturnRepository returnRepository,
                        ReturnItemRepository returnItemRepository,
                        ReceiptRepository receiptRepository,
                        CreditNoteRepository creditNoteRepository,
                        ProductService productService,
                        SaleStateMachine stateMachine,
                        ReceiptGenerator receiptGenerator) {
        this.saleRepository = saleRepository;
        this.returnRepository = returnRepository;
        this.returnItemRepository = returnItemRepository;
        this.receiptRepository = receiptRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.productService = productService;
        this.stateMachine = stateMachine;
        this.receiptGenerator = receiptGenerator;
    }
    
    /**
     * Processes a full return for a completed sale.
     * 
     * @param saleId The sale ID
     * @param reason The return reason
     * @return The return result
     */
    public ReturnResult processFullReturn(Long saleId, String reason) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        
        // Validate return eligibility
        validateReturnEligibility(sale);
        
        // Create full return
        FullReturn returnEntity = new FullReturn(sale, reason);
        
        // Add all items to return
        for (SaleItem item : sale.getItems()) {
            ReturnItem returnItem = new ReturnItem(returnEntity, item, item.getQuantity(), reason);
            returnEntity.addItem(returnItem);
        }
        
        returnRepository.save(returnEntity);
        
        // Increment stock
        incrementStockForSaleItems(sale.getItems());
        
        // Update sale status
        sale.markAsReturned();
        saleRepository.save(sale);
        
        // Generate credit note if credit sale
        CreditNote creditNote = null;
        if (sale.getPaymentType() == PaymentType.CREDIT) {
            creditNote = receiptGenerator.generateCreditNote(returnEntity);
            creditNoteRepository.save(creditNote);
            returnEntity.setCreditNoteNumber(creditNote.getCreditNoteNumber());
        }
        
        // Generate return receipt
        Receipt receipt = receiptGenerator.generateReturnReceipt(sale, returnEntity);
        receiptRepository.save(receipt);
        
        return new ReturnResult(returnEntity, receipt, creditNote);
    }
    
    /**
     * Processes a partial return for specific items.
     * 
     * @param saleId The sale ID
     * @param items The items to return with quantities and reasons
     * @return The return result
     */
    public ReturnResult processPartialReturn(Long saleId, List<PartialReturnItem> items) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        
        // Validate return eligibility
        validateReturnEligibility(sale);
        
        // Create partial return
        PartialReturn returnEntity = new PartialReturn(sale, "Partial return");
        
        // Validate and add items
        for (PartialReturnItem item : items) {
            // Find the sale item
            SaleItem saleItem = sale.getItems().stream()
                .filter(si -> si.getId().equals(item.saleItemId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("SaleItem", item.saleItemId()));
            
            // Check if return quantity is valid
            Integer totalReturned = returnItemRepository.getTotalReturnedQuantityForItem(item.saleItemId());
            Integer remaining = saleItem.getQuantity() - (totalReturned != null ? totalReturned : 0);
            
            if (item.quantity() > remaining) {
                throw new IllegalArgumentException(
                    "Cannot return " + item.quantity() + " items. Maximum returnable: " + remaining
                );
            }
            
            ReturnItem returnItem = new ReturnItem(returnEntity, saleItem, item.quantity(), item.reason());
            returnEntity.addItem(returnItem);
        }
        
        returnRepository.save(returnEntity);
        
        // Increment stock for returned items only
        incrementStockForReturnItems(returnEntity.getItems());
        
        // Update sale status
        // Check if all items have been returned
        boolean allReturned = areAllItemsReturned(sale, returnEntity);
        if (allReturned) {
            sale.markAsReturned();
        } else {
            sale.markAsPartiallyReturned();
        }
        saleRepository.save(sale);
        
        // Generate credit note if credit sale
        CreditNote creditNote = null;
        if (sale.getPaymentType() == PaymentType.CREDIT) {
            creditNote = receiptGenerator.generateCreditNote(returnEntity);
            creditNoteRepository.save(creditNote);
            returnEntity.setCreditNoteNumber(creditNote.getCreditNoteNumber());
        }
        
        // Generate return receipt
        Receipt receipt = receiptGenerator.generateReturnReceipt(sale, returnEntity);
        receiptRepository.save(receipt);
        
        return new ReturnResult(returnEntity, receipt, creditNote);
    }
    
    /**
     * Validates that a sale can be returned.
     */
    private void validateReturnEligibility(Sale sale) {
        if (!stateMachine.canReturn(sale.getStatus())) {
            throw new InvalidSaleStateException(
                "Cannot return sale with status: " + sale.getStatus() + 
                ". Only COMPLETED or PARTIALLY_RETURNED sales can be returned."
            );
        }
    }
    
    /**
     * Increments stock for sale items (full return).
     */
    private void incrementStockForSaleItems(List<SaleItem> items) {
        List<StockAdjustment> adjustments = items.stream()
            .map(item -> new StockAdjustment(item.getProductId(), item.getQuantity()))
            .collect(Collectors.toList());
        
        try {
            productService.incrementStock(adjustments);
        } catch (ExternalServiceException e) {
            throw new IllegalStateException("Cannot increment stock: " + e.getMessage(), e);
        }
    }
    
    /**
     * Increments stock for return items (partial return).
     */
    private void incrementStockForReturnItems(List<ReturnItem> items) {
        List<StockAdjustment> adjustments = items.stream()
            .map(item -> new StockAdjustment(item.getSaleItem().getProductId(), item.getQuantity()))
            .collect(Collectors.toList());
        
        try {
            productService.incrementStock(adjustments);
        } catch (ExternalServiceException e) {
            throw new IllegalStateException("Cannot increment stock: " + e.getMessage(), e);
        }
    }
    
    /**
     * Checks if all items in the sale have been returned.
     */
    private boolean areAllItemsReturned(Sale sale, Return newReturn) {
        for (SaleItem saleItem : sale.getItems()) {
            Integer totalReturned = returnItemRepository.getTotalReturnedQuantityForItem(saleItem.getId());
            if (totalReturned == null || totalReturned < saleItem.getQuantity()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Record for partial return item.
     */
    public record PartialReturnItem(
        Long saleItemId,
        Integer quantity,
        String reason
    ) {}
    
    /**
     * Record for return result.
     */
    public record ReturnResult(
        Return returnEntity,
        Receipt receipt,
        CreditNote creditNote
    ) {}
}
