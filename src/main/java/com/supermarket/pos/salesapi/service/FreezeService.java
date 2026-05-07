package com.supermarket.pos.salesapi.service;

import com.supermarket.pos.salesapi.component.InvalidSaleStateException;
import com.supermarket.pos.salesapi.component.SaleStateMachine;
import com.supermarket.pos.salesapi.exception.ResourceNotFoundException;
import com.supermarket.pos.salesapi.model.entity.Sale;
import com.supermarket.pos.salesapi.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for freezing and resuming sales.
 */
@Service
@Transactional
public class FreezeService {
    
    private final SaleRepository saleRepository;
    private final SaleStateMachine stateMachine;
    
    public FreezeService(SaleRepository saleRepository, SaleStateMachine stateMachine) {
        this.saleRepository = saleRepository;
        this.stateMachine = stateMachine;
    }
    
    /**
     * Freezes an active sale.
     * 
     * @param saleId The sale ID
     * @return The frozen sale
     */
    public Sale freezeSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        
        if (!stateMachine.canFreeze(sale.getStatus())) {
            throw new InvalidSaleStateException(
                "Cannot freeze sale with status: " + sale.getStatus() + 
                ". Only ACTIVE sales can be frozen."
            );
        }
        
        sale.freeze();
        
        return saleRepository.save(sale);
    }
    
    /**
     * Resumes a frozen sale.
     * 
     * @param saleId The sale ID
     * @return The resumed sale
     */
    public Sale resumeSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        
        if (!stateMachine.canResume(sale.getStatus())) {
            throw new InvalidSaleStateException(
                "Cannot resume sale with status: " + sale.getStatus() + 
                ". Only FROZEN sales can be resumed."
            );
        }
        
        sale.resume();
        
        return saleRepository.save(sale);
    }
    
    /**
     * Gets all frozen sales for a terminal.
     * 
     * @param terminalId The terminal ID
     * @return List of frozen sales
     */
    @Transactional(readOnly = true)
    public List<Sale> getFrozenSalesByTerminal(String terminalId) {
        return saleRepository.findByStatusAndTerminalIdOrderByCreatedAtDesc(
            com.supermarket.pos.salesapi.model.enums.SaleStatus.FROZEN, terminalId
        );
    }
}
