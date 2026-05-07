package com.supermarket.pos.salesapi.model.entity;

import com.supermarket.pos.salesapi.model.enums.ReturnType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Represents a partial return where specific items and quantities are returned.
 */
@Entity
@DiscriminatorValue("PARTIAL")
public class PartialReturn extends Return {
    
    protected PartialReturn() {}
    
    /**
     * Creates a partial return for a sale.
     * 
     * @param sale The sale to return items from
     * @param reason The reason for the return
     */
    public PartialReturn(Sale sale, String reason) {
        super(sale, reason);
    }
}
