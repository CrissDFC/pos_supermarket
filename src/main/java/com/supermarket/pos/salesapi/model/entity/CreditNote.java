package com.supermarket.pos.salesapi.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a credit note generated for returns on credit sales.
 */
@Entity
@Table(name = "credit_notes",
       uniqueConstraints = @UniqueConstraint(name = "uk_credit_note_number", columnNames = "credit_note_number"))
public class CreditNote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "credit_note_number", nullable = false, unique = true, length = 50)
    private String creditNoteNumber;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false, unique = true)
    private Return returnEntity;
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
    
    // Default constructor for JPA
    protected CreditNote() {}
    
    /**
     * Creates a new CreditNote for a return.
     * 
     * @param creditNoteNumber The unique credit note number
     * @param returnEntity The return this credit note is for
     * @param amount The credit note amount
     */
    public CreditNote(String creditNoteNumber, Return returnEntity, BigDecimal amount) {
        this.creditNoteNumber = creditNoteNumber;
        this.returnEntity = returnEntity;
        this.amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.generatedAt = LocalDateTime.now();
    }
    
    // Getters
    public Long getId() { return id; }
    public String getCreditNoteNumber() { return creditNoteNumber; }
    public Return getReturnEntity() { return returnEntity; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreditNote that = (CreditNote) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
