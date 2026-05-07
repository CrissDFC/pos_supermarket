package com.supermarket.pos.salesapi.repository;

import com.supermarket.pos.salesapi.model.entity.Return;
import com.supermarket.pos.salesapi.model.entity.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Return entities.
 */
@Repository
public interface ReturnRepository extends JpaRepository<Return, Long> {
    
    /**
     * Finds all returns for a specific sale.
     * 
     * @param saleId The sale ID
     * @return List of returns
     */
    List<Return> findBySaleId(Long saleId);
    
    /**
     * Finds a return by credit note number.
     * 
     * @param creditNoteNumber The credit note number
     * @return The return if found
     */
    Optional<Return> findByCreditNoteNumber(String creditNoteNumber);
    
    /**
     * Checks if a sale has any returns.
     * 
     * @param saleId The sale ID
     * @return true if the sale has returns
     */
    boolean existsBySaleId(Long saleId);
}
