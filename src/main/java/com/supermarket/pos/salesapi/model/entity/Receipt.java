package com.supermarket.pos.salesapi.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a receipt generated upon successful checkout.
 */
@Entity
@Table(name = "receipts", 
       uniqueConstraints = @UniqueConstraint(name = "uk_receipt_number", columnNames = "receipt_number"))
public class Receipt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "receipt_number", nullable = false, unique = true, length = 50)
    private String receiptNumber;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false, unique = true)
    private Sale sale;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
    
    // Default constructor for JPA
    protected Receipt() {}
    
    /**
     * Creates a new Receipt for a sale.
     * 
     * @param receiptNumber The unique receipt number
     * @param sale The sale this receipt is for
     * @param content The receipt content
     */
    public Receipt(String receiptNumber, Sale sale, String content) {
        this.receiptNumber = receiptNumber;
        this.sale = sale;
        this.content = content;
        this.generatedAt = LocalDateTime.now();
    }
    
    // Getters
    public Long getId() { return id; }
    public String getReceiptNumber() { return receiptNumber; }
    public Sale getSale() { return sale; }
    public String getContent() { return content; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Receipt receipt = (Receipt) o;
        return Objects.equals(id, receipt.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
