package com.supermarket.pos.salesapi.job;

import com.supermarket.pos.salesapi.model.entity.Sale;
import com.supermarket.pos.salesapi.model.enums.SaleStatus;
import com.supermarket.pos.salesapi.repository.SaleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that cancels frozen sales that have exceeded the expiration time.
 * 
 * Default expiration time: 2 hours
 * Runs every minute
 */
@Component
public class FrozenSaleExpirationJob {
    
    private static final Logger logger = LoggerFactory.getLogger(FrozenSaleExpirationJob.class);
    
    private final SaleRepository saleRepository;
    private final int expirationHours;
    private static final String CANCELLATION_REASON = "Automatic cancellation due to expiration";
    
    public FrozenSaleExpirationJob(SaleRepository saleRepository,
                                   @Value("${app.frozen-sale.expiration-hours:2}") int expirationHours) {
        this.saleRepository = saleRepository;
        this.expirationHours = expirationHours;
    }
    
    /**
     * Cancels expired frozen sales.
     * Runs every minute.
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void cancelExpiredFrozenSales() {
        LocalDateTime expirationThreshold = LocalDateTime.now().minusHours(expirationHours);
        
        List<Sale> expiredSales = saleRepository.findByStatusAndFrozenAtBefore(
            SaleStatus.FROZEN, 
            expirationThreshold
        );
        
        if (!expiredSales.isEmpty()) {
            logger.info("Found {} expired frozen sales to cancel", expiredSales.size());
            
            for (Sale sale : expiredSales) {
                sale.cancel(CANCELLATION_REASON);
                saleRepository.save(sale);
                logger.debug("Cancelled expired frozen sale: {}", sale.getId());
            }
            
            logger.info("Cancelled {} expired frozen sales", expiredSales.size());
        }
    }
}
