package com.supermarket.pos.salesapi.repository;

import com.supermarket.pos.salesapi.model.entity.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for CreditNote entities.
 */
@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {
    
    /**
     * Finds a credit note by return ID.
     * 
     * @param returnId The return ID
     * @return The credit note if found
     */
    Optional<CreditNote> findByReturnEntityId(Long returnId);
    
    /**
     * Finds a credit note by credit note number.
     * 
     * @param creditNoteNumber The credit note number
     * @return The credit note if found
     */
    Optional<CreditNote> findByCreditNoteNumber(String creditNoteNumber);
}
