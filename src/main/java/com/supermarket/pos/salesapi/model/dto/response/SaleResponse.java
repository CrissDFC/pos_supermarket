package com.supermarket.pos.salesapi.model.dto.response;

import com.supermarket.pos.salesapi.model.entity.Sale;
import com.supermarket.pos.salesapi.model.entity.SaleItem;
import com.supermarket.pos.salesapi.model.enums.PaymentType;
import com.supermarket.pos.salesapi.model.enums.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for sale data.
 */
public record SaleResponse(
    Long id,
    String terminalId,
    String cashierId,
    Long customerId,
    SaleStatus status,
    BigDecimal subtotal,
    BigDecimal taxRate,
    BigDecimal taxAmount,
    BigDecimal discountAmount,
    BigDecimal total,
    PaymentType paymentType,
    String transactionId,
    List<SaleItemResponse> items,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {
    public static SaleResponse from(Sale sale) {
        return new SaleResponse(
            sale.getId(),
            sale.getTerminalId(),
            sale.getCashierId(),
            sale.getCustomerId(),
            sale.getStatus(),
            sale.getSubtotal(),
            sale.getTaxRate(),
            sale.getTaxAmount(),
            sale.getDiscountAmount(),
            sale.getTotal(),
            sale.getPaymentType(),
            sale.getTransactionId(),
            sale.getItems().stream()
                .map(SaleItemResponse::from)
                .collect(Collectors.toList()),
            sale.getCreatedAt(),
            sale.getCompletedAt()
        );
    }
    
    public record SaleItemResponse(
        Long id,
        Long productId,
        String productName,
        String barcode,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal
    ) {
        public static SaleItemResponse from(SaleItem item) {
            return new SaleItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getBarcode(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal()
            );
        }
    }
}
