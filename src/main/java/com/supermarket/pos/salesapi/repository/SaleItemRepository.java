package com.supermarket.pos.salesapi.repository;

import com.supermarket.pos.salesapi.model.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SaleItem entities.
 */
@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    
    /**
     * Finds all items for a specific sale.
     * 
     * @param saleId The sale ID
     * @return List of sale items
     */
    List<SaleItem> findBySaleId(Long saleId);
    
    /**
     * Finds a specific item in a sale by product ID.
     * 
     * @param saleId The sale ID
     * @param productId The product ID
     * @return The sale item if found
     */
    Optional<SaleItem> findBySaleIdAndProductId(Long saleId, Long productId);
    
    /**
     * Deletes a specific item from a sale.
     * 
     * @param saleId The sale ID
     * @param itemId The item ID
     * @return Number of deleted records
     */
    long deleteBySaleIdAndId(Long saleId, Long itemId);
    
    /**
     * Counts items in a sale.
     * 
     * @param saleId The sale ID
     * @return The count of items
     */
    long countBySaleId(Long saleId);
}
