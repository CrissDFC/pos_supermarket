package com.supermarket.pos.salesapi.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a single item within a return transaction.
 */
@Entity
@Table(name = "return_items")
public class ReturnItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private Return returnEntity;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_item_id", nullable = false)
    private SaleItem saleItem;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "reason", nullable = false, length = 255)
    private String reason;
    
    @Column(name = "returned_at", nullable = false)
    private LocalDateTime returnedAt;
    
    // Default constructor for JPA
    protected ReturnItem() {}
    
    /**
     * Creates a new ReturnItem.
     * 
     * @param returnEntity The return this item belongs to
     * @param saleItem The original sale item being returned
     * @param quantity The quantity being returned
     * @param reason The reason for the return
     */
    public ReturnItem(Return returnEntity, SaleItem saleItem, Integer quantity, String reason) {
        this.returnEntity = returnEntity;
        this.saleItem = saleItem;
        this.quantity = quantity;
        this.amount = saleItem.getUnitPrice().multiply(BigDecimal.valueOf(quantity))
                              .setScale(2, BigDecimal.ROUND_HALF_UP);
        this.reason = reason;
        this.returnedAt = LocalDateTime.now();
    }
    
    // Getters
    public Long getId() { return id; }
    public Return getReturnEntity() { return returnEntity; }
    public SaleItem getSaleItem() { return saleItem; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getAmount() { return amount; }
    public String getReason() { return reason; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    
    // Package-private setter
    void setReturnEntity(Return returnEntity) {
        this.returnEntity = returnEntity;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReturnItem that = (ReturnItem) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
