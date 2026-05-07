package com.supermarket.pos.salesapi.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a line item within a sale, containing product information,
 * quantity, and pricing snapshot at the time of addition.
 */
@Entity
@Table(name = "sale_items")
public class SaleItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;
    
    @Column(name = "barcode", length = 50)
    private String barcode;
    
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;
    
    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;
    
    // Default constructor for JPA
    protected SaleItem() {}
    
    /**
     * Creates a new SaleItem with calculated line total.
     * 
     * @param sale The sale this item belongs to
     * @param productId The product ID
     * @param productName The product name snapshot
     * @param barcode The product barcode
     * @param unitPrice The unit price snapshot
     * @param quantity The quantity (must be >= 1)
     */
    public SaleItem(Sale sale, Long productId, String productName, String barcode, 
                    BigDecimal unitPrice, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        this.sale = sale;
        this.productId = productId;
        this.productName = productName;
        this.barcode = barcode;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = calculateLineTotal(unitPrice, quantity);
        this.addedAt = LocalDateTime.now();
    }
    
    /**
     * Calculates the line total using BigDecimal with 2 decimal precision.
     */
    private BigDecimal calculateLineTotal(BigDecimal unitPrice, Integer quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity))
                       .setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Updates the quantity and recalculates the line total.
     * 
     * @param newQuantity The new quantity (must be >= 1)
     */
    public void updateQuantity(Integer newQuantity) {
        if (newQuantity == null || newQuantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        this.quantity = newQuantity;
        this.lineTotal = calculateLineTotal(this.unitPrice, newQuantity);
    }
    
    /**
     * Increments the quantity by the specified amount.
     * 
     * @param additionalQuantity The quantity to add
     */
    public void incrementQuantity(Integer additionalQuantity) {
        if (additionalQuantity == null || additionalQuantity < 1) {
            throw new IllegalArgumentException("Additional quantity must be at least 1");
        }
        this.quantity += additionalQuantity;
        this.lineTotal = calculateLineTotal(this.unitPrice, this.quantity);
    }
    
    // Getters
    public Long getId() { return id; }
    public Sale getSale() { return sale; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getBarcode() { return barcode; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public LocalDateTime getAddedAt() { return addedAt; }
    
    // Package-private setter for sale (used by JPA)
    void setSale(Sale sale) {
        this.sale = sale;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaleItem saleItem = (SaleItem) o;
        return Objects.equals(id, saleItem.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
