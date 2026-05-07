package com.supermarket.pos.salesapi.service;

import com.supermarket.pos.salesapi.client.ExternalServiceException;
import com.supermarket.pos.salesapi.client.ProductApiClient;
import com.supermarket.pos.salesapi.model.dto.ProductSummary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for product-related operations.
 * Delegates to the external Product API.
 */
@Service
public class ProductService {
    
    private final ProductApiClient productApiClient;
    
    public ProductService(ProductApiClient productApiClient) {
        this.productApiClient = productApiClient;
    }
    
    /**
     * Searches for products by name.
     * 
     * @param name The product name to search for
     * @return List of matching products
     * @throws ExternalServiceException if the Product API is unavailable
     */
    public List<ProductSummary> searchProductsByName(String name) {
        return productApiClient.searchProductsByName(name);
    }
    
    /**
     * Searches for products by barcode.
     * 
     * @param barcode The barcode to search for
     * @return List of matching products
     * @throws ExternalServiceException if the Product API is unavailable
     */
    public List<ProductSummary> searchProductsByBarcode(String barcode) {
        return productApiClient.searchProductsByBarcode(barcode);
    }
    
    /**
     * Gets a product by ID.
     * 
     * @param productId The product ID
     * @return The product
     * @throws ExternalServiceException if the Product API is unavailable
     */
    public ProductSummary getProduct(Long productId) {
        return productApiClient.getProduct(productId);
    }
    
    /**
     * Checks if a product has sufficient stock.
     * 
     * @param productId The product ID
     * @param requestedQuantity The requested quantity
     * @return true if stock is sufficient
     */
    public boolean hasSufficientStock(Long productId, Integer requestedQuantity) {
        var stockInfo = productApiClient.checkStock(productId);
        return stockInfo.availableQuantity() >= requestedQuantity;
    }
    
    /**
     * Decrements stock for products during checkout.
     * 
     * @param adjustments The stock adjustments
     */
    public void decrementStock(List<com.supermarket.pos.salesapi.model.dto.StockAdjustment> adjustments) {
        productApiClient.decrementStock(adjustments);
    }
    
    /**
     * Increments stock for products during returns.
     * 
     * @param adjustments The stock adjustments
     */
    public void incrementStock(List<com.supermarket.pos.salesapi.model.dto.StockAdjustment> adjustments) {
        productApiClient.incrementStock(adjustments);
    }
}
