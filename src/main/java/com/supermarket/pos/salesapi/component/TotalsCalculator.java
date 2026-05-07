package com.supermarket.pos.salesapi.component;

import com.supermarket.pos.salesapi.model.entity.SaleItem;
import com.supermarket.pos.salesapi.model.valueobject.Discount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calculates sale totals with BigDecimal precision.
 * 
 * All calculations use:
 * - BigDecimal with scale 2
 * - HALF_UP rounding mode
 * - Integer arithmetic internally (cents) to avoid floating-point errors
 */
@Component
public class TotalsCalculator {
    
    private final BigDecimal defaultTaxRate;
    
    public TotalsCalculator(@Value("${app.tax.rate:0.19}") BigDecimal defaultTaxRate) {
        this.defaultTaxRate = defaultTaxRate;
    }
    
    /**
     * Calculates the line total for an item.
     * 
     * @param unitPrice The unit price
     * @param quantity The quantity
     * @return The line total with 2 decimal precision
     */
    public BigDecimal calculateLineTotal(BigDecimal unitPrice, Integer quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity))
                       .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates the subtotal as the sum of all line totals.
     * 
     * @param items The list of sale items
     * @return The subtotal with 2 decimal precision
     */
    public BigDecimal calculateSubtotal(List<SaleItem> items) {
        return items.stream()
                   .map(SaleItem::getLineTotal)
                   .reduce(BigDecimal.ZERO, BigDecimal::add)
                   .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates the tax amount based on the subtotal.
     * 
     * @param subtotal The subtotal
     * @param taxRate The tax rate (e.g., 0.19 for 19%)
     * @return The tax amount with 2 decimal precision
     */
    public BigDecimal calculateTax(BigDecimal subtotal, BigDecimal taxRate) {
        return subtotal.multiply(taxRate)
                      .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates the tax amount using the default tax rate.
     * 
     * @param subtotal The subtotal
     * @return The tax amount with 2 decimal precision
     */
    public BigDecimal calculateTax(BigDecimal subtotal) {
        return calculateTax(subtotal, defaultTaxRate);
    }
    
    /**
     * Calculates the discount amount.
     * 
     * @param subtotal The subtotal
     * @param discount The discount (percentage or fixed amount)
     * @return The discount amount with 2 decimal precision
     */
    public BigDecimal calculateDiscount(BigDecimal subtotal, Discount discount) {
        if (discount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        
        BigDecimal discountAmount = discount.calculateAmount(subtotal);
        
        // Ensure discount doesn't exceed subtotal
        if (discountAmount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException(
                "Discount amount cannot exceed subtotal. Maximum allowed: " + subtotal
            );
        }
        
        return discountAmount.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates the total.
     * 
     * @param subtotal The subtotal
     * @param tax The tax amount
     * @param discount The discount amount
     * @return The total with 2 decimal precision
     */
    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal tax, BigDecimal discount) {
        BigDecimal total = subtotal.add(tax).subtract(discount);
        return total.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates all totals for a list of items.
     * 
     * @param items The list of sale items
     * @param taxRate The tax rate (optional, uses default if null)
     * @param discount The discount (optional)
     * @return Totals containing subtotal, tax, discount, and total
     */
    public Totals calculateAllTotals(List<SaleItem> items, BigDecimal taxRate, Discount discount) {
        BigDecimal subtotal = calculateSubtotal(items);
        BigDecimal effectiveTaxRate = taxRate != null ? taxRate : defaultTaxRate;
        BigDecimal tax = calculateTax(subtotal, effectiveTaxRate);
        BigDecimal discountAmount = calculateDiscount(subtotal, discount);
        BigDecimal total = calculateTotal(subtotal, tax, discountAmount);
        
        return new Totals(subtotal, tax, discountAmount, total);
    }
    
    /**
     * Calculates all totals with default tax rate and no discount.
     * 
     * @param items The list of sale items
     * @return Totals containing subtotal, tax, and total
     */
    public Totals calculateAllTotals(List<SaleItem> items) {
        return calculateAllTotals(items, null, null);
    }
    
    /**
     * Record containing all calculated totals.
     */
    public record Totals(
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal total
    ) {}
}
