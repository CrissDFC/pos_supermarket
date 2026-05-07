package com.supermarket.pos.salesapi.component;

import com.supermarket.pos.salesapi.model.entity.*;
import com.supermarket.pos.salesapi.model.enums.PaymentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Generates receipts for completed sales and returns.
 */
@Component
public class ReceiptGenerator {
    
    private final String storeName;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public ReceiptGenerator(@Value("${app.store.name:SuperMarket POS}") String storeName) {
        this.storeName = storeName;
    }
    
    /**
     * Generates a unique transaction ID.
     * 
     * @return A unique transaction ID
     */
    public String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Generates a unique receipt number.
     * 
     * @return A unique receipt number
     */
    public String generateReceiptNumber() {
        return "RCP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Generates a unique credit reference number.
     * 
     * @return A unique credit reference number
     */
    public String generateCreditReferenceNumber() {
        return "CRD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Generates a unique credit note number.
     * 
     * @return A unique credit note number
     */
    public String generateCreditNoteNumber() {
        return "CN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Generates a checkout receipt for a completed sale.
     * 
     * @param sale The completed sale
     * @return The receipt entity
     */
    public Receipt generateCheckoutReceipt(Sale sale) {
        String receiptNumber = generateReceiptNumber();
        String content = buildCheckoutReceiptContent(sale);
        return new Receipt(receiptNumber, sale, content);
    }
    
    /**
     * Generates a return receipt.
     * 
     * @param sale The sale being returned
     * @param returnEntity The return transaction
     * @return The receipt entity
     */
    public Receipt generateReturnReceipt(Sale sale, Return returnEntity) {
        String receiptNumber = generateReceiptNumber();
        String content = buildReturnReceiptContent(sale, returnEntity);
        return new Receipt(receiptNumber, sale, content);
    }
    
    /**
     * Generates a credit note for a return on a credit sale.
     * 
     * @param returnEntity The return transaction
     * @return The credit note entity
     */
    public CreditNote generateCreditNote(Return returnEntity) {
        String creditNoteNumber = generateCreditNoteNumber();
        return new CreditNote(creditNoteNumber, returnEntity, returnEntity.getReturnAmount());
    }
    
    /**
     * Builds the checkout receipt content.
     */
    private String buildCheckoutReceiptContent(Sale sale) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("================================\n");
        sb.append(storeName).append("\n");
        sb.append("================================\n\n");
        sb.append("Terminal: ").append(sale.getTerminalId()).append("\n");
        sb.append("Cashier: ").append(sale.getCashierId()).append("\n");
        sb.append("Date: ").append(sale.getCompletedAt().format(DATE_TIME_FORMATTER)).append("\n");
        
        if (sale.getCustomerId() != null) {
            sb.append("Customer ID: ").append(sale.getCustomerId()).append("\n");
        }
        
        sb.append("\n--------------------------------\n");
        sb.append("ITEMS:\n");
        sb.append("--------------------------------\n");
        
        for (SaleItem item : sale.getItems()) {
            sb.append(String.format("%s\n", item.getProductName()));
            sb.append(String.format("  %d x $%.2f = $%.2f\n", 
                item.getQuantity(), 
                item.getUnitPrice(), 
                item.getLineTotal()));
        }
        
        sb.append("--------------------------------\n");
        sb.append(String.format("Subtotal:     $%.2f\n", sale.getSubtotal()));
        sb.append(String.format("Tax (19%%):    $%.2f\n", sale.getTaxAmount()));
        
        if (sale.getDiscountAmount() != null && sale.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Discount:    -$%.2f\n", sale.getDiscountAmount()));
        }
        
        sb.append(String.format("TOTAL:        $%.2f\n", sale.getTotal()));
        
        sb.append("\n--------------------------------\n");
        sb.append("Payment Method: ").append(sale.getPaymentType()).append("\n");
        
        if (sale.getPaymentType() == PaymentType.CASH) {
            sb.append(String.format("Amount Received: $%.2f\n", sale.getAmountReceived()));
            sb.append(String.format("Change:          $%.2f\n", sale.getChangeAmount()));
        } else if (sale.getPaymentType() == PaymentType.CREDIT) {
            sb.append("Credit Reference: ").append(sale.getCreditReference()).append("\n");
        }
        
        sb.append("\n--------------------------------\n");
        sb.append("Transaction ID: ").append(sale.getTransactionId()).append("\n");
        sb.append("================================\n");
        sb.append("    Thank you for your purchase!    \n");
        sb.append("================================\n");
        
        return sb.toString();
    }
    
    /**
     * Builds the return receipt content.
     */
    private String buildReturnReceiptContent(Sale sale, Return returnEntity) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("================================\n");
        sb.append(storeName).append(" - RETURN\n");
        sb.append("================================\n\n");
        sb.append("Original Transaction: ").append(sale.getTransactionId()).append("\n");
        sb.append("Return Date: ").append(returnEntity.getReturnedAt().format(DATE_TIME_FORMATTER)).append("\n");
        sb.append("Reason: ").append(returnEntity.getReason()).append("\n");
        
        sb.append("\n--------------------------------\n");
        sb.append("RETURNED ITEMS:\n");
        sb.append("--------------------------------\n");
        
        for (ReturnItem item : returnEntity.getItems()) {
            sb.append(String.format("%s\n", item.getSaleItem().getProductName()));
            sb.append(String.format("  Qty: %d, Amount: $%.2f\n", 
                item.getQuantity(), 
                item.getAmount()));
        }
        
        sb.append("--------------------------------\n");
        sb.append(String.format("RETURN TOTAL: $%.2f\n", returnEntity.getReturnAmount()));
        
        if (returnEntity.getCreditNoteNumber() != null) {
            sb.append("\nCredit Note: ").append(returnEntity.getCreditNoteNumber()).append("\n");
        }
        
        sb.append("================================\n");
        
        return sb.toString();
    }
}
