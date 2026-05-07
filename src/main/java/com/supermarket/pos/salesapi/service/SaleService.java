package com.supermarket.pos.salesapi.service;

import com.supermarket.pos.salesapi.client.ExternalServiceException;
import com.supermarket.pos.salesapi.component.InvalidSaleStateException;
import com.supermarket.pos.salesapi.component.ReceiptGenerator;
import com.supermarket.pos.salesapi.component.SaleStateMachine;
import com.supermarket.pos.salesapi.component.TotalsCalculator;
import com.supermarket.pos.salesapi.exception.ResourceNotFoundException;
import com.supermarket.pos.salesapi.model.dto.ProductSummary;
import com.supermarket.pos.salesapi.model.dto.StockAdjustment;
import com.supermarket.pos.salesapi.model.entity.Sale;
import com.supermarket.pos.salesapi.model.entity.SaleItem;
import com.supermarket.pos.salesapi.model.enums.SaleStatus;
import com.supermarket.pos.salesapi.model.valueobject.Discount;
import com.supermarket.pos.salesapi.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for sale-related operations.
 */
@Service
@Transactional
public class SaleService {
    
    private final SaleRepository saleRepository;
    private final ProductService productService;
    private final CustomerService customerService;
    private final SaleStateMachine stateMachine;
    private final TotalsCalculator totalsCalculator;
    private final ReceiptGenerator receiptGenerator;
    
    public SaleService(SaleRepository saleRepository,
                      ProductService productService,
                      CustomerService customerService,
                      SaleStateMachine stateMachine,
                      TotalsCalculator totalsCalculator,
                      ReceiptGenerator receiptGenerator) {
        this.saleRepository = saleRepository;
        this.productService = productService;
        this.customerService = customerService;
        this.stateMachine = stateMachine;
        this.totalsCalculator = totalsCalculator;
        this.receiptGenerator = receiptGenerator;
    }
    
    /**
     * Creates a new sale.
     * 
     * @param terminalId The POS terminal ID
     * @param cashierId The cashier ID
     * @param customerId Optional customer ID
     * @return The created sale
     */
    public Sale createSale(String terminalId, String cashierId, Long customerId) {
        Sale sale;
        if (customerId != null) {
            // Validate customer exists
            try {
                customerService.getCustomer(customerId);
            } catch (ExternalServiceException e) {
                throw new IllegalStateException("Cannot verify customer: " + e.getMessage(), e);
            }
            sale = new Sale(terminalId, cashierId, customerId);
        } else {
            sale = new Sale(terminalId, cashierId);
        }
        
        return saleRepository.save(sale);
    }
    
    /**
     * Gets a sale by ID.
     * 
     * @param saleId The sale ID
     * @return The sale
     * @throws ResourceNotFoundException if sale not found
     */
    @Transactional(readOnly = true)
    public Sale getSale(Long saleId) {
        return saleRepository.findById(saleId)
            .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
    }
    
    /**
     * Adds an item to a sale.
     * 
     * @param saleId The sale ID
     * @param productId The product ID
     * @param quantity The quantity (must be >= 1)
     * @return The updated sale
     * @throws InvalidSaleStateException if sale cannot be modified
     */
    public Sale addItem(Long saleId, Long productId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        
        Sale sale = getSale(saleId);
        
        if (!stateMachine.canModify(sale.getStatus())) {
            throw new InvalidSaleStateException(
                "Cannot add items to sale with status: " + sale.getStatus()
            );
        }
        
        // Get product details
        ProductSummary product = productService.getProduct(productId);
        
        // Check stock availability
        if (!productService.hasSufficientStock(productId, quantity)) {
            throw new IllegalStateException(
                "Insufficient stock for product: " + product.name() + 
                ". Available: " + product.availableStock()
            );
        }
        
        // Create or update item
        SaleItem existingItem = sale.findItemByProductId(productId);
        if (existingItem != null) {
            existingItem.incrementQuantity(quantity);
        } else {
            SaleItem newItem = new SaleItem(
                sale,
                product.id(),
                product.name(),
                product.barcode(),
                product.unitPrice(),
                quantity
            );
            sale.addItem(newItem);
        }
        
        // Recalculate totals
        recalculateTotals(sale);
        
        return saleRepository.save(sale);
    }
    
    /**
     * Updates the quantity of an item in a sale.
     * 
     * @param saleId The sale ID
     * @param itemId The item ID
     * @param newQuantity The new quantity (must be >= 1)
     * @return The updated sale
     */
    public Sale updateItemQuantity(Long saleId, Long itemId, Integer newQuantity) {
        if (newQuantity == null || newQuantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        
        Sale sale = getSale(saleId);
        
        if (!stateMachine.canModify(sale.getStatus())) {
            throw new InvalidSaleStateException(
                "Cannot update items in sale with status: " + sale.getStatus()
            );
        }
        
        // Find item and update quantity
        sale.getItems().stream()
            .filter(item -> item.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("SaleItem", itemId))
            .updateQuantity(newQuantity);
        
        // Recalculate totals
        recalculateTotals(sale);
        
        return saleRepository.save(sale);
    }
    
    /**
     * Removes an item from a sale.
     * 
     * @param saleId The sale ID
     * @param itemId The item ID
     * @return The updated sale
     */
    public Sale removeItem(Long saleId, Long itemId) {
        Sale sale = getSale(saleId);
        
        if (!stateMachine.canModify(sale.getStatus())) {
            throw new InvalidSaleStateException(
                "Cannot remove items from sale with status: " + sale.getStatus()
            );
        }
        
        sale.removeItem(itemId);
        
        // Recalculate totals
        recalculateTotals(sale);
        
        return saleRepository.save(sale);
    }
    
    /**
     * Applies a discount to a sale.
     * 
     * @param saleId The sale ID
     * @param discount The discount to apply
     * @return The updated sale
     */
    public Sale applyDiscount(Long saleId, Discount discount) {
        Sale sale = getSale(saleId);
        
        if (!stateMachine.canModify(sale.getStatus())) {
            throw new InvalidSaleStateException(
                "Cannot apply discount to sale with status: " + sale.getStatus()
            );
        }
        
        if (discount.isPercentage()) {
            sale.setDiscountPercentage(discount.getPercentage());
        } else {
            sale.setDiscountAmount(discount.getFixedAmount());
        }
        
        // Recalculate totals
        recalculateTotals(sale);
        
        return saleRepository.save(sale);
    }
    
    /**
     * Associates a customer with a sale.
     * 
     * @param saleId The sale ID
     * @param customerId The customer ID
     * @return The updated sale
     */
    public Sale associateCustomer(Long saleId, Long customerId) {
        Sale sale = getSale(saleId);
        
        if (!stateMachine.canModify(sale.getStatus())) {
            throw new InvalidSaleStateException(
                "Cannot associate customer to sale with status: " + sale.getStatus()
            );
        }
        
        // Validate customer exists
        try {
            customerService.getCustomer(customerId);
        } catch (ExternalServiceException e) {
            throw new IllegalStateException("Cannot verify customer: " + e.getMessage(), e);
        }
        
        sale.setCustomerId(customerId);
        
        return saleRepository.save(sale);
    }
    
    /**
     * Cancels a sale.
     * 
     * @param saleId The sale ID
     * @param reason The cancellation reason
     * @return The cancelled sale
     */
    public Sale cancelSale(Long saleId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason is required");
        }
        if (reason.length() > 255) {
            throw new IllegalArgumentException("Cancellation reason must not exceed 255 characters");
        }
        
        Sale sale = getSale(saleId);
        
        if (!stateMachine.canCancel(sale.getStatus())) {
            throw new InvalidSaleStateException(
                "Cannot cancel sale with status: " + sale.getStatus()
            );
        }
        
        sale.cancel(reason);
        
        return saleRepository.save(sale);
    }
    
    /**
     * Gets all frozen sales for a terminal.
     * 
     * @param terminalId The terminal ID
     * @return List of frozen sales
     */
    @Transactional(readOnly = true)
    public List<Sale> getFrozenSalesByTerminal(String terminalId) {
        return saleRepository.findByStatusAndTerminalIdOrderByCreatedAtDesc(
            SaleStatus.FROZEN, terminalId
        );
    }
    
    /**
     * Recalculates totals for a sale.
     */
    private void recalculateTotals(Sale sale) {
        if (!sale.hasItems()) {
            sale.setTotals(
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                sale.getDiscountAmount() != null ? sale.getDiscountAmount() : BigDecimal.ZERO,
                BigDecimal.ZERO.setScale(2)
            );
            return;
        }
        
        TotalsCalculator.Totals totals = totalsCalculator.calculateAllTotals(
            sale.getItems(),
            sale.getTaxRate(),
            sale.getDiscountPercentage() != null ? 
                Discount.percentage(sale.getDiscountPercentage()) :
                (sale.getDiscountAmount() != null && sale.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) ?
                    Discount.fixedAmount(sale.getDiscountAmount()) : null
        );
        
        sale.setTotals(totals.subtotal(), totals.tax(), totals.discount(), totals.total());
    }
}
