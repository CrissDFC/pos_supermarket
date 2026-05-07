package com.supermarket.pos.salesapi.service;

import com.supermarket.pos.salesapi.client.ExternalServiceException;
import com.supermarket.pos.salesapi.component.InvalidSaleStateException;
import com.supermarket.pos.salesapi.component.ReceiptGenerator;
import com.supermarket.pos.salesapi.component.SaleStateMachine;
import com.supermarket.pos.salesapi.exception.ResourceNotFoundException;
import com.supermarket.pos.salesapi.model.dto.StockAdjustment;
import com.supermarket.pos.salesapi.model.entity.Receipt;
import com.supermarket.pos.salesapi.model.entity.Sale;
import com.supermarket.pos.salesapi.model.enums.CreditStatus;
import com.supermarket.pos.salesapi.model.enums.PaymentType;
import com.supermarket.pos.salesapi.repository.ReceiptRepository;
import com.supermarket.pos.salesapi.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for processing payments and checkouts.
 */
@Service
@Transactional
public class PaymentService {
    
    private final SaleRepository saleRepository;
    private final ReceiptRepository receiptRepository;
    private final ProductService productService;
    private final CustomerService customerService;
    private final SaleStateMachine stateMachine;
    private final ReceiptGenerator receiptGenerator;
    
    public PaymentService(SaleRepository saleRepository,
                         ReceiptRepository receiptRepository,
                         ProductService productService,
                         CustomerService customerService,
                         SaleStateMachine stateMachine,
                         ReceiptGenerator receiptGenerator) {
        this.saleRepository = saleRepository;
        this.receiptRepository = receiptRepository;
        this.productService = productService;
        this.customerService = customerService;
        this.stateMachine = stateMachine;
        this.receiptGenerator = receiptGenerator;
    }
    
    /**
     * Processes a cash checkout.
     * 
     * @param saleId The sale ID
     * @param amountReceived The amount received from customer
     * @return The completed sale
     */
    public CheckoutResult processCashCheckout(Long saleId, BigDecimal amountReceived) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        
        // Validate sale can be checked out
        validateCheckoutEligibility(sale);
        
        // Validate amount received
        if (amountReceived.compareTo(sale.getTotal()) < 0) {
            throw new IllegalArgumentException(
                "Amount received must be at least " + sale.getTotal() + 
                ". Received: " + amountReceived
            );
        }
        
        // Validate and decrement stock
        validateAndDecrementStock(sale);
        
        // Calculate change
        BigDecimal change = amountReceived.subtract(sale.getTotal());
        
        // Generate transaction ID
        String transactionId = receiptGenerator.generateTransactionId();
        
        // Complete the sale
        sale.completeCashPayment(amountReceived, change, transactionId);
        saleRepository.save(sale);
        
        // Generate receipt
        Receipt receipt = receiptGenerator.generateCheckoutReceipt(sale);
        receiptRepository.save(receipt);
        
        return new CheckoutResult(sale, receipt, change);
    }
    
    /**
     * Processes a credit checkout.
     * 
     * @param saleId The sale ID
     * @return The completed sale
     */
    public CheckoutResult processCreditCheckout(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        
        // Validate sale can be checked out
        validateCheckoutEligibility(sale);
        
        // Validate customer is associated
        if (sale.getCustomerId() == null) {
            throw new IllegalStateException("Credit sale requires a customer to be associated");
        }
        
        // Verify credit status
        CreditStatus creditStatus;
        try {
            creditStatus = customerService.verifyCreditStatus(sale.getCustomerId());
        } catch (ExternalServiceException e) {
            throw new IllegalStateException("Cannot verify credit status: " + e.getMessage(), e);
        }
        
        if (creditStatus != CreditStatus.APPROVED) {
            throw new IllegalStateException(
                "Customer credit status is not APPROVED. Current status: " + creditStatus
            );
        }
        
        // Validate and decrement stock
        validateAndDecrementStock(sale);
        
        // Generate IDs
        String transactionId = receiptGenerator.generateTransactionId();
        String creditReference = receiptGenerator.generateCreditReferenceNumber();
        
        // Complete the sale
        sale.completeCreditPayment(creditReference, transactionId);
        saleRepository.save(sale);
        
        // Generate receipt
        Receipt receipt = receiptGenerator.generateCheckoutReceipt(sale);
        receiptRepository.save(receipt);
        
        return new CheckoutResult(sale, receipt, null);
    }
    
    /**
     * Validates that a sale can be checked out.
     */
    private void validateCheckoutEligibility(Sale sale) {
        if (!stateMachine.canCheckout(sale.getStatus())) {
            throw new InvalidSaleStateException(
                "Cannot checkout sale with status: " + sale.getStatus()
            );
        }
        
        if (!sale.hasItems()) {
            throw new IllegalStateException("Cannot checkout a sale with no items");
        }
    }
    
    /**
     * Validates stock availability and decrements stock for all items.
     */
    private void validateAndDecrementStock(Sale sale) {
        // Build stock adjustments
        List<StockAdjustment> adjustments = sale.getItems().stream()
            .map(item -> new StockAdjustment(item.getProductId(), item.getQuantity()))
            .collect(Collectors.toList());
        
        // Validate stock for each item
        for (var item : sale.getItems()) {
            if (!productService.hasSufficientStock(item.getProductId(), item.getQuantity())) {
                throw new IllegalStateException(
                    "Insufficient stock for product: " + item.getProductName()
                );
            }
        }
        
        // Decrement stock
        try {
            productService.decrementStock(adjustments);
        } catch (ExternalServiceException e) {
            throw new IllegalStateException("Cannot decrement stock: " + e.getMessage(), e);
        }
    }
    
    /**
     * Record for checkout result.
     */
    public record CheckoutResult(
        Sale sale,
        Receipt receipt,
        BigDecimal change
    ) {}
}
