package com.supermarket.pos.salesapi.controller;

import com.supermarket.pos.salesapi.model.dto.ProductSummary;
import com.supermarket.pos.salesapi.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for product search operations.
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product search operations")
public class ProductController {
    
    private final ProductService productService;
    
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    /**
     * Searches for products by name or barcode.
     */
    @GetMapping("/search")
    @Operation(summary = "Search products by name or barcode")
    public ResponseEntity<List<ProductSummary>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String barcode) {
        
        List<ProductSummary> products;
        
        if (barcode != null && !barcode.trim().isEmpty()) {
            products = productService.searchProductsByBarcode(barcode);
        } else if (name != null && !name.trim().isEmpty()) {
            products = productService.searchProductsByName(name);
        } else {
            products = List.of();
        }
        
        return ResponseEntity.ok(products);
    }
}
