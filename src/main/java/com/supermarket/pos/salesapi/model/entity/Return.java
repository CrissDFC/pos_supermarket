package com.supermarket.pos.salesapi.model.entity;

import com.supermarket.pos.salesapi.model.enums.ReturnType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a return transaction for a completed sale.
 */
@Entity
@Table(name = "returns")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "return_type", discriminatorType = DiscriminatorType.STRING)
public class Return {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "return_type", insertable = false, updatable = false)
    private ReturnType type;
    
    @Column(name = "return_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal returnAmount;
    
    @Column(name = "reason", nullable = false, length = 255)
    private String reason;
    
    @Column(name = "credit_note_number", length = 50)
    private String creditNoteNumber;
    
    @Column(name = "returned_at", nullable = false)
    private LocalDateTime returnedAt;
    
    @OneToMany(mappedBy = "returnEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReturnItem> items = new ArrayList<>();
    
    // Default constructor for JPA
    protected Return() {}
    
    /**
     * Creates a new Return for a sale.
     * 
     * @param sale The sale being returned
     * @param reason The reason for the return
     */
    public Return(Sale sale, String reason) {
        this.sale = sale;
        this.reason = reason;
        this.returnedAt = LocalDateTime.now();
        this.returnAmount = BigDecimal.ZERO.setScale(2);
    }
    
    /**
     * Adds a return item.
     * 
     * @param item The item to add
     */
    public void addItem(ReturnItem item) {
        items.add(item);
        item.setReturnEntity(this);
        recalculateAmount();
    }
    
    /**
     * Recalculates the total return amount based on items.
     */
    private void recalculateAmount() {
        this.returnAmount = items.stream()
                                  .map(ReturnItem::getAmount)
                                  .reduce(BigDecimal.ZERO, BigDecimal::add)
                                  .setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Sets the credit note number for credit sales.
     * 
     * @param creditNoteNumber The credit note number
     */
    public void setCreditNoteNumber(String creditNoteNumber) {
        this.creditNoteNumber = creditNoteNumber;
    }
    
    // Getters
    public Long getId() { return id; }
    public Sale getSale() { return sale; }
    public ReturnType getType() { return type; }
    public BigDecimal getReturnAmount() { return returnAmount; }
    public String getReason() { return reason; }
    public String getCreditNoteNumber() { return creditNoteNumber; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public List<ReturnItem> getItems() { return new ArrayList<>(items); }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Return aReturn = (Return) o;
        return Objects.equals(id, aReturn.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
