package com.supermarket.pos.salesapi.repository;

import com.supermarket.pos.salesapi.model.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Receipt entities.
 */
@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    
    /**
     * Finds a receipt by sale ID.
     * 
     * @param saleId The sale ID
     * @return The receipt if found
     */
    Optional<Receipt> findBySaleId(Long saleId);
    
    /**
     * Finds a receipt by receipt number.
     * 
     * @param receiptNumber The receipt number
     * @return The receipt if found
     */
    Optional<Receipt> findByReceiptNumber(String receiptNumber);
}
