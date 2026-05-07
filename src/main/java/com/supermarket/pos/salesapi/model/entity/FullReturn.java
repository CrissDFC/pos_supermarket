package com.supermarket.pos.salesapi.model.entity;

import com.supermarket.pos.salesapi.model.enums.ReturnType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Represents a full return where all items in a sale are returned.
 */
@Entity
@DiscriminatorValue("FULL")
public class FullReturn extends Return {
    
    protected FullReturn() {}
    
    /**
     * Creates a full return for a sale.
     * 
     * @param sale The sale to return
     * @param reason The reason for the return
     */
    public FullReturn(Sale sale, String reason) {
        super(sale, reason);
    }
}
