package com.supermarket.pos.salesapi.repository;

import com.supermarket.pos.salesapi.model.entity.Sale;
import com.supermarket.pos.salesapi.model.enums.SaleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Sale entities.
 */
@Repository
public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {
    
    /**
     * Finds a sale by ID with optimistic locking.
     * 
     * @param id The sale ID
     * @return The sale if found
     */
    @Lock(LockModeType.OPTIMISTIC)
    Optional<Sale> findById(Long id);
    
    /**
     * Finds all frozen sales for a specific terminal.
     * 
     * @param status The sale status (FROZEN)
     * @param terminalId The terminal ID
     * @return List of frozen sales ordered by creation date
     */
    List<Sale> findByStatusAndTerminalIdOrderByCreatedAtDesc(SaleStatus status, String terminalId);
    
    /**
     * Finds all sales that are frozen and have been frozen before a certain time.
     * Used for the expiration job.
     * 
     * @param status The sale status (FROZEN)
     * @param frozenBefore The timestamp threshold
     * @return List of expired frozen sales
     */
    @Query("SELECT s FROM Sale s WHERE s.status = :status AND s.frozenAt < :frozenBefore")
    List<Sale> findByStatusAndFrozenAtBefore(@Param("status") SaleStatus status, 
                                              @Param("frozenBefore") LocalDateTime frozenBefore);
    
    /**
     * Counts sales by status for a specific terminal.
     * 
     * @param status The sale status
     * @param terminalId The terminal ID
     * @return The count of sales
     */
    long countByStatusAndTerminalId(SaleStatus status, String terminalId);
    
    /**
     * Finds sales by customer ID.
     * 
     * @param customerId The customer ID
     * @return List of sales for the customer
     */
    List<Sale> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    
    /**
     * Finds a sale by transaction ID.
     * 
     * @param transactionId The transaction ID
     * @return The sale if found
     */
    Optional<Sale> findByTransactionId(String transactionId);
    
    /**
     * Updates the status of expired frozen sales to CANCELLED.
     * 
     * @param status The current status (FROZEN)
     * @param frozenBefore The timestamp threshold
     * @param newStatus The new status (CANCELLED)
     * @param reason The cancellation reason
     * @param cancelledAt The cancellation timestamp
     * @return Number of updated records
     */
    @Modifying
    @Query("UPDATE Sale s SET s.status = :newStatus, s.cancellationReason = :reason, " +
           "s.cancelledAt = :cancelledAt WHERE s.status = :status AND s.frozenAt < :frozenBefore")
    int cancelExpiredFrozenSales(@Param("status") SaleStatus status,
                                  @Param("frozenBefore") LocalDateTime frozenBefore,
                                  @Param("newStatus") SaleStatus newStatus,
                                  @Param("reason") String reason,
                                  @Param("cancelledAt") LocalDateTime cancelledAt);
}
