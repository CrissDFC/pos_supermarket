package com.supermarket.pos.salesapi.component;

import com.supermarket.pos.salesapi.model.enums.SaleStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages valid state transitions for a sale.
 * 
 * Valid transitions:
 * - ACTIVE → COMPLETED, CANCELLED, FROZEN
 * - FROZEN → ACTIVE, CANCELLED
 * - COMPLETED → RETURNED, PARTIALLY_RETURNED
 * - PARTIALLY_RETURNED → RETURNED
 */
@Component
public class SaleStateMachine {
    
    private final Map<SaleStatus, Set<SaleStatus>> allowedTransitions = new EnumMap<>(SaleStatus.class);
    
    public SaleStateMachine() {
        // Define valid state transitions
        allowedTransitions.put(SaleStatus.ACTIVE, 
            EnumSet.of(SaleStatus.COMPLETED, SaleStatus.CANCELLED, SaleStatus.FROZEN));
        
        allowedTransitions.put(SaleStatus.FROZEN, 
            EnumSet.of(SaleStatus.ACTIVE, SaleStatus.CANCELLED));
        
        allowedTransitions.put(SaleStatus.COMPLETED, 
            EnumSet.of(SaleStatus.RETURNED, SaleStatus.PARTIALLY_RETURNED));
        
        allowedTransitions.put(SaleStatus.PARTIALLY_RETURNED, 
            EnumSet.of(SaleStatus.RETURNED, SaleStatus.PARTIALLY_RETURNED));
    }
    
    /**
     * Checks if a transition from the current status to the target status is allowed.
     * 
     * @param currentStatus The current sale status
     * @param targetStatus The target sale status
     * @return true if the transition is allowed
     */
    public boolean canTransition(SaleStatus currentStatus, SaleStatus targetStatus) {
        Set<SaleStatus> allowedTargets = allowedTransitions.get(currentStatus);
        return allowedTargets != null && allowedTargets.contains(targetStatus);
    }
    
    /**
     * Validates a state transition and throws an exception if invalid.
     * 
     * @param currentStatus The current sale status
     * @param targetStatus The target sale status
     * @throws InvalidSaleStateException if the transition is not allowed
     */
    public void validateTransition(SaleStatus currentStatus, SaleStatus targetStatus) {
        if (!canTransition(currentStatus, targetStatus)) {
            throw new InvalidSaleStateException(
                String.format("Cannot transition from %s to %s", currentStatus, targetStatus)
            );
        }
    }
    
    /**
     * Checks if a sale can be cancelled.
     * Only ACTIVE or FROZEN sales can be cancelled.
     * 
     * @param currentStatus The current sale status
     * @return true if the sale can be cancelled
     */
    public boolean canCancel(SaleStatus currentStatus) {
        return currentStatus == SaleStatus.ACTIVE || currentStatus == SaleStatus.FROZEN;
    }
    
    /**
     * Checks if a sale can be frozen.
     * Only ACTIVE sales can be frozen.
     * 
     * @param currentStatus The current sale status
     * @return true if the sale can be frozen
     */
    public boolean canFreeze(SaleStatus currentStatus) {
        return currentStatus == SaleStatus.ACTIVE;
    }
    
    /**
     * Checks if a sale can be resumed.
     * Only FROZEN sales can be resumed.
     * 
     * @param currentStatus The current sale status
     * @return true if the sale can be resumed
     */
    public boolean canResume(SaleStatus currentStatus) {
        return currentStatus == SaleStatus.FROZEN;
    }
    
    /**
     * Checks if a sale can be returned.
     * Only COMPLETED or PARTIALLY_RETURNED sales can be returned.
     * 
     * @param currentStatus The current sale status
     * @return true if the sale can be returned
     */
    public boolean canReturn(SaleStatus currentStatus) {
        return currentStatus == SaleStatus.COMPLETED || currentStatus == SaleStatus.PARTIALLY_RETURNED;
    }
    
    /**
     * Checks if a sale can be modified.
     * Only ACTIVE sales can be modified.
     * 
     * @param currentStatus The current sale status
     * @return true if the sale can be modified
     */
    public boolean canModify(SaleStatus currentStatus) {
        return currentStatus == SaleStatus.ACTIVE;
    }
    
    /**
     * Checks if a sale can be checked out.
     * Only ACTIVE sales can be checked out.
     * 
     * @param currentStatus The current sale status
     * @return true if the sale can be checked out
     */
    public boolean canCheckout(SaleStatus currentStatus) {
        return currentStatus == SaleStatus.ACTIVE;
    }
}
