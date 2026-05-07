package com.supermarket.pos.salesapi.client;

import com.supermarket.pos.salesapi.model.dto.ProductSummary;
import com.supermarket.pos.salesapi.model.dto.StockAdjustment;
import com.supermarket.pos.salesapi.model.dto.StockInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Client for communicating with the external Product API.
 * 
 * Uses Resilience4j for fault tolerance:
 * - Circuit breaker: Opens after 50% failure rate
 * - Retry: 3 attempts with 100ms wait
 * - Time limiter: 10 second timeout
 */
@Component
public class ProductApiClient {
    
    private final RestTemplate restTemplate;
    private final String productApiBaseUrl;
    
    public ProductApiClient(RestTemplate restTemplate,
                           @Value("${external-api.product.base-url}") String productApiBaseUrl) {
        this.restTemplate = restTemplate;
        this.productApiBaseUrl = productApiBaseUrl;
    }
    
    /**
     * Searches for products by name (partial match, case-insensitive).
     * 
     * @param name The product name to search for
     * @return List of matching products
     */
    @CircuitBreaker(name = "productApi", fallbackMethod = "searchProductsFallback")
    @Retry(name = "productApi")
    public List<ProductSummary> searchProductsByName(String name) {
        String url = productApiBaseUrl + "/api/v1/products/search?name=" + name;
        ProductSummary[] products = restTemplate.getForObject(url, ProductSummary[].class);
        return products != null ? Arrays.asList(products) : Collections.emptyList();
    }
    
    /**
     * Searches for a product by barcode (exact match).
     * 
     * @param barcode The barcode to search for
     * @return List containing the matching product (empty if not found)
     */
    @CircuitBreaker(name = "productApi", fallbackMethod = "searchProductsFallback")
    @Retry(name = "productApi")
    public List<ProductSummary> searchProductsByBarcode(String barcode) {
        String url = productApiBaseUrl + "/api/v1/products/search?barcode=" + barcode;
        ProductSummary[] products = restTemplate.getForObject(url, ProductSummary[].class);
        return products != null ? Arrays.asList(products) : Collections.emptyList();
    }
    
    /**
     * Gets a product by ID.
     * 
     * @param productId The product ID
     * @return The product if found
     */
    @CircuitBreaker(name = "productApi", fallbackMethod = "getProductFallback")
    @Retry(name = "productApi")
    public ProductSummary getProduct(Long productId) {
        String url = productApiBaseUrl + "/api/v1/products/" + productId;
        return restTemplate.getForObject(url, ProductSummary.class);
    }
    
    /**
     * Checks stock availability for a product.
     * 
     * @param productId The product ID
     * @return Stock information
     */
    @CircuitBreaker(name = "productApi", fallbackMethod = "checkStockFallback")
    @Retry(name = "productApi")
    public StockInfo checkStock(Long productId) {
        String url = productApiBaseUrl + "/api/v1/products/" + productId + "/stock";
        return restTemplate.getForObject(url, StockInfo.class);
    }
    
    /**
     * Decrements stock for multiple products.
     * Called during checkout to reserve inventory.
     * 
     * @param adjustments List of stock adjustments (negative quantities)
     */
    @CircuitBreaker(name = "productApi", fallbackMethod = "decrementStockFallback")
    @Retry(name = "productApi")
    public void decrementStock(List<StockAdjustment> adjustments) {
        String url = productApiBaseUrl + "/api/v1/products/stock/decrement";
        restTemplate.postForObject(url, adjustments, Void.class);
    }
    
    /**
     * Increments stock for multiple products.
     * Called during returns to restore inventory.
     * 
     * @param adjustments List of stock adjustments (positive quantities)
     */
    @CircuitBreaker(name = "productApi", fallbackMethod = "incrementStockFallback")
    @Retry(name = "productApi")
    public void incrementStock(List<StockAdjustment> adjustments) {
        String url = productApiBaseUrl + "/api/v1/products/stock/increment";
        restTemplate.postForObject(url, adjustments, Void.class);
    }
    
    // Fallback methods
    
    private List<ProductSummary> searchProductsFallback(String query, Throwable t) {
        throw new ExternalServiceException("Product API is temporarily unavailable", "ProductApi", t);
    }
    
    private ProductSummary getProductFallback(Long productId, Throwable t) {
        throw new ExternalServiceException("Product API is temporarily unavailable", "ProductApi", t);
    }
    
    private StockInfo checkStockFallback(Long productId, Throwable t) {
        throw new ExternalServiceException("Product API is temporarily unavailable", "ProductApi", t);
    }
    
    private void decrementStockFallback(List<StockAdjustment> adjustments, Throwable t) {
        throw new ExternalServiceException("Product API is temporarily unavailable", "ProductApi", t);
    }
    
    private void incrementStockFallback(List<StockAdjustment> adjustments, Throwable t) {
        throw new ExternalServiceException("Product API is temporarily unavailable", "ProductApi", t);
    }
}
