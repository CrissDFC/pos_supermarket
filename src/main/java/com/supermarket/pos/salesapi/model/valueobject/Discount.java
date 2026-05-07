package com.supermarket.pos.salesapi.model.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing a discount that can be either a percentage or fixed amount.
 * 
 * Discount rules:
 * - Percentage discount: 0% to 100%
 * - Fixed amount discount: 0 to subtotal amount
 */
public class Discount {
    private final BigDecimal percentage;
    private final BigDecimal fixedAmount;
    
    private Discount(BigDecimal percentage, BigDecimal fixedAmount) {
        this.percentage = percentage;
        this.fixedAmount = fixedAmount;
    }
    
    /**
     * Creates a percentage-based discount.
     * 
     * @param percentage The discount percentage (0 to 100)
     * @return A new Discount instance
     * @throws IllegalArgumentException if percentage is out of range
     */
    public static Discount percentage(BigDecimal percentage) {
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100");
        }
        return new Discount(percentage, null);
    }
    
    /**
     * Creates a fixed amount discount.
     * 
     * @param fixedAmount The fixed discount amount (must be non-negative)
     * @return A new Discount instance
     * @throws IllegalArgumentException if fixed amount is negative
     */
    public static Discount fixedAmount(BigDecimal fixedAmount) {
        if (fixedAmount == null || fixedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fixed amount must be non-negative");
        }
        return new Discount(null, fixedAmount);
    }
    
    /**
     * Creates a no-discount instance.
     * 
     * @return A Discount instance with zero value
     */
    public static Discount none() {
        return new Discount(BigDecimal.ZERO, null);
    }
    
    public boolean isPercentage() {
        return percentage != null;
    }
    
    public boolean isFixedAmount() {
        return fixedAmount != null;
    }
    
    public BigDecimal getPercentage() {
        return percentage;
    }
    
    public BigDecimal getFixedAmount() {
        return fixedAmount;
    }
    
    /**
     * Calculates the discount amount for a given subtotal.
     * 
     * @param subtotal The subtotal to apply the discount to
     * @return The calculated discount amount
     */
    public BigDecimal calculateAmount(BigDecimal subtotal) {
        if (isPercentage()) {
            return subtotal.multiply(percentage).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
        } else if (isFixedAmount()) {
            return fixedAmount;
        }
        return BigDecimal.ZERO;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Discount discount = (Discount) o;
        return Objects.equals(percentage, discount.percentage) &&
               Objects.equals(fixedAmount, discount.fixedAmount);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(percentage, fixedAmount);
    }
    
    @Override
    public String toString() {
        if (isPercentage()) {
            return "Discount{percentage=" + percentage + "%}";
        } else if (isFixedAmount()) {
            return "Discount{fixedAmount=" + fixedAmount + "}";
        }
        return "Discount{none}";
    }
}
