package com.supermarket.pos.salesapi.repository;

import com.supermarket.pos.salesapi.model.entity.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ReturnItem entities.
 */
@Repository
public interface ReturnItemRepository extends JpaRepository<ReturnItem, Long> {
    
    /**
     * Finds all return items for a specific return.
     * 
     * @param returnId The return ID
     * @return List of return items
     */
    List<ReturnItem> findByReturnEntityId(Long returnId);
    
    /**
     * Gets the total returned quantity for a specific sale item.
     * 
     * @param saleItemId The sale item ID
     * @return The total quantity returned
     */
    @Query("SELECT COALESCE(SUM(ri.quantity), 0) FROM ReturnItem ri WHERE ri.saleItem.id = :saleItemId")
    Integer getTotalReturnedQuantityForItem(@Param("saleItemId") Long saleItemId);
}
