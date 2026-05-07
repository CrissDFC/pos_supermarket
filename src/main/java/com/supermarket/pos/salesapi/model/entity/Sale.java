package com.supermarket.pos.salesapi.model.entity;

import com.supermarket.pos.salesapi.model.enums.PaymentType;
import com.supermarket.pos.salesapi.model.enums.SaleStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a sales transaction at a POS terminal.
 * 
 * A sale goes through various states in its lifecycle:
 * ACTIVE → COMPLETED, CANCELLED, or FROZEN
 * FROZEN → ACTIVE
 * COMPLETED → RETURNED or PARTIALLY_RETURNED
 */
@Entity
@Table(name = "sales")
public class Sale {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "terminal_id", nullable = false, length = 50)
    private String terminalId;
    
    @Column(name = "cashier_id", nullable = false, length = 50)
    private String cashierId;
    
    @Column(name = "customer_id")
    private Long customerId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SaleStatus status;
    
    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;
    
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate;
    
    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;
    
    @Column(name = "discount_amount", precision = 19, scale = 2)
    private BigDecimal discountAmount;
    
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;
    
    @Column(name = "total", nullable = false, precision = 19, scale = 2)
    private BigDecimal total;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", length = 10)
    private PaymentType paymentType;
    
    @Column(name = "amount_received", precision = 19, scale = 2)
    private BigDecimal amountReceived;
    
    @Column(name = "change_amount", precision = 19, scale = 2)
    private BigDecimal changeAmount;
    
    @Column(name = "credit_reference", length = 50)
    private String creditReference;
    
    @Column(name = "transaction_id", length = 50)
    private String transactionId;
    
    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "frozen_at")
    private LocalDateTime frozenAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Version
    private Long version;
    
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SaleItem> items = new ArrayList<>();
    
    // Default constructor for JPA
    protected Sale() {}
    
    /**
     * Creates a new Sale with ACTIVE status and zero totals.
     * 
     * @param terminalId The POS terminal ID
     * @param cashierId The cashier ID
     */
    public Sale(String terminalId, String cashierId) {
        this.terminalId = terminalId;
        this.cashierId = cashierId;
        this.status = SaleStatus.ACTIVE;
        this.subtotal = BigDecimal.ZERO.setScale(2);
        this.taxRate = new BigDecimal("0.19"); // Default 19%
        this.taxAmount = BigDecimal.ZERO.setScale(2);
        this.discountAmount = BigDecimal.ZERO.setScale(2);
        this.total = BigDecimal.ZERO.setScale(2);
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * Creates a new Sale with an associated customer.
     * 
     * @param terminalId The POS terminal ID
     * @param cashierId The cashier ID
     * @param customerId The customer ID
     */
    public Sale(String terminalId, String cashierId, Long customerId) {
        this(terminalId, cashierId);
        this.customerId = customerId;
    }
    
    /**
     * Adds an item to the sale. If the product already exists,
     * increments the quantity instead of adding a duplicate.
     * 
     * @param item The item to add
     */
    public void addItem(SaleItem item) {
        // Check if product already exists in sale
        items.stream()
             .filter(existingItem -> existingItem.getProductId().equals(item.getProductId()))
             .findFirst()
             .ifPresentOrElse(
                 existingItem -> existingItem.incrementQuantity(item.getQuantity()),
                 () -> {
                     items.add(item);
                     item.setSale(this);
                 }
             );
    }
    
    /**
     * Removes an item from the sale.
     * 
     * @param itemId The item ID to remove
     * @return true if the item was removed, false if not found
     */
    public boolean removeItem(Long itemId) {
        return items.removeIf(item -> item.getId().equals(itemId));
    }
    
    /**
     * Finds an item by product ID.
     * 
     * @param productId The product ID
     * @return The item if found, null otherwise
     */
    public SaleItem findItemByProductId(Long productId) {
        return items.stream()
                    .filter(item -> item.getProductId().equals(productId))
                    .findFirst()
                    .orElse(null);
    }
    
    /**
     * Checks if the sale has any items.
     * 
     * @return true if the sale has items, false otherwise
     */
    public boolean hasItems() {
        return !items.isEmpty();
    }
    
    /**
     * Checks if the sale can be modified.
     * Only ACTIVE sales can be modified.
     * 
     * @return true if the sale can be modified
     */
    public boolean canBeModified() {
        return status == SaleStatus.ACTIVE;
    }
    
    // Getters
    public Long getId() { return id; }
    public String getTerminalId() { return terminalId; }
    public String getCashierId() { return cashierId; }
    public Long getCustomerId() { return customerId; }
    public SaleStatus getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTaxRate() { return taxRate; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public BigDecimal getTotal() { return total; }
    public PaymentType getPaymentType() { return paymentType; }
    public BigDecimal getAmountReceived() { return amountReceived; }
    public BigDecimal getChangeAmount() { return changeAmount; }
    public String getCreditReference() { return creditReference; }
    public String getTransactionId() { return transactionId; }
    public String getCancellationReason() { return cancellationReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getFrozenAt() { return frozenAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public List<SaleItem> getItems() { return new ArrayList<>(items); }
    public Long getVersion() { return version; }
    
    // Setters for state transitions and updates
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public void setDiscountPercentage(BigDecimal discountPercentage) { this.discountPercentage = discountPercentage; }
    
    /**
     * Sets the calculated totals for the sale.
     */
    public void setTotals(BigDecimal subtotal, BigDecimal taxAmount, BigDecimal discountAmount, BigDecimal total) {
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.discountAmount = discountAmount;
        this.total = total;
    }
    
    /**
     * Completes the sale with payment information.
     */
    public void complete(PaymentType paymentType, String transactionId) {
        this.status = SaleStatus.COMPLETED;
        this.paymentType = paymentType;
        this.transactionId = transactionId;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Completes a cash payment.
     */
    public void completeCashPayment(BigDecimal amountReceived, BigDecimal change, String transactionId) {
        this.amountReceived = amountReceived;
        this.changeAmount = change;
        complete(PaymentType.CASH, transactionId);
    }
    
    /**
     * Completes a credit payment.
     */
    public void completeCreditPayment(String creditReference, String transactionId) {
        this.creditReference = creditReference;
        complete(PaymentType.CREDIT, transactionId);
    }
    
    /**
     * Cancels the sale with a reason.
     */
    public void cancel(String cancellationReason) {
        this.status = SaleStatus.CANCELLED;
        this.cancellationReason = cancellationReason;
        this.cancelledAt = LocalDateTime.now();
    }
    
    /**
     * Freezes the sale.
     */
    public void freeze() {
        this.status = SaleStatus.FROZEN;
        this.frozenAt = LocalDateTime.now();
    }
    
    /**
     * Resumes a frozen sale.
     */
    public void resume() {
        this.status = SaleStatus.ACTIVE;
        this.frozenAt = null;
    }
    
    /**
     * Marks the sale as fully returned.
     */
    public void markAsReturned() {
        this.status = SaleStatus.RETURNED;
    }
    
    /**
     * Marks the sale as partially returned.
     */
    public void markAsPartiallyReturned() {
        this.status = SaleStatus.PARTIALLY_RETURNED;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sale sale = (Sale) o;
        return Objects.equals(id, sale.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
