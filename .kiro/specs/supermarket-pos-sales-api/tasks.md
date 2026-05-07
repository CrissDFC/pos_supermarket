# Implementation Plan: Supermarket POS Sales API

## Overview

This implementation plan covers the development of a Spring Boot 3.x REST API for managing supermarket POS sales transactions. The API handles sale creation, item management, payment processing (cash and credit), freezing/resuming transactions, cancellation, and returns (full and partial).

The implementation follows a layered architecture: Controller → Service → Repository, with external API clients for Product and Customer APIs using Resilience4j patterns.

## Tasks

- [ ] 1. Set up project structure and configuration
  - [ ] 1.1 Initialize Spring Boot project with dependencies
    - Create Maven/Gradle build file with Spring Boot 3.x, Spring Data JPA, Spring Web, Validation, H2/PostgreSQL drivers, Resilience4j, SpringDoc OpenAPI, and jqwik for property-based testing
    - Configure application.yml with datasource, JPA, and application-specific settings (tax rate, frozen sale expiration, store name)
    - Set up application-dev.yml for H2 database and application-prod.yml for PostgreSQL
    - _Requirements: 18.1, 18.2, 18.3_

  - [ ] 1.2 Create base package structure
    - Create packages: controller, service, repository, model/entity, dto, client, exception, config, component
    - Create main application class with component scanning
    - _Requirements: Architecture_

  - [ ] 1.3 Configure Resilience4j circuit breakers and retry
    - Create CircuitBreakerConfig for Product API and Customer API
    - Create RetryConfig with 3 max attempts and 100ms wait duration
    - Configure TimeLimiter for 10-second timeout
    - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5_

  - [ ] 1.4 Configure OpenAPI/Swagger documentation
    - Configure SpringDoc OpenAPI with API info and security schemes
    - Enable Swagger UI at /swagger-ui.html
    - _Requirements: API Documentation_

- [ ] 2. Implement domain models and entities
  - [ ] 2.1 Create Sale entity with all fields
    - Define Sale entity with @Entity, @Table annotations
    - Include all fields: id, terminalId, cashierId, customerId, status, subtotal, taxRate, taxAmount, discountAmount, discountPercentage, total, paymentType, amountReceived, changeAmount, creditReference, transactionId, cancellationReason, timestamps, version
    - Add @Version for optimistic locking
    - Configure BigDecimal precision (19,2) for monetary fields
    - _Requirements: 3.1, 3.3, 3.4, 3.5_

  - [ ] 2.2 Create SaleItem entity
    - Define SaleItem entity with @ManyToOne relationship to Sale
    - Include fields: id, sale, productId, productName, barcode, unitPrice, quantity, lineTotal, addedAt
    - Configure CASCADE ALL and orphan removal on Sale.items
    - _Requirements: 4.1, 4.8_

  - [ ] 2.3 Create Return and ReturnItem entities
    - Define Return entity with type, returnAmount, reason, creditNoteNumber, returnedAt
    - Define ReturnItem entity with quantity, amount, reason
    - Configure relationships: Return -> Sale, ReturnItem -> Return, ReturnItem -> SaleItem
    - _Requirements: 11.1, 12.1_

  - [ ] 2.4 Create Receipt and CreditNote entities
    - Define Receipt entity with receiptNumber, content, generatedAt, and sale relationship
    - Define CreditNote entity with creditNoteNumber, amount, generatedAt, and return relationship
    - _Requirements: 14.1, 14.2, 14.3_

  - [ ] 2.5 Create enums and value objects
    - Define SaleStatus enum: ACTIVE, COMPLETED, CANCELLED, FROZEN, RETURNED, PARTIALLY_RETURNED
    - Define PaymentType enum: CASH, CREDIT
    - Define ReturnType enum: FULL, PARTIAL
    - Define CreditStatus enum: APPROVED, REJECTED, PENDING
    - Define Discount value object with percentage and fixed amount support
    - _Requirements: 3.1, 6.1, 7.1, 11.1_

  - [ ]* 2.6 Write property tests for entity invariants
    - **Property 40: Receipt Number Uniqueness** - Verify receipt number uniqueness constraint
    - **Validates: Requirements 14.3**

- [ ] 3. Implement repository layer
  - [ ] 3.1 Create SaleRepository with custom queries
    - Extend JpaRepository<Sale, Long> and JpaSpecificationExecutor<Sale>
    - Add @Lock(LockModeType.OPTIMISTIC) on findById
    - Create query methods: findByStatusAndTerminalIdOrderByCreatedAtDesc, findByStatusAndFrozenAtBefore
    - Create custom update query for status transition to CANCELLED
    - _Requirements: 3.1, 9.1, 10.6_

  - [ ] 3.2 Create SaleItemRepository
    - Extend JpaRepository<SaleItem, Long>
    - Create methods: findBySaleId, findBySaleIdAndProductId, deleteBySaleIdAndItemId
    - _Requirements: 4.1, 4.3_

  - [ ] 3.3 Create ReturnRepository with stock tracking
    - Extend JpaRepository<Return, Long>
    - Create methods: findBySaleId, getTotalReturnedQuantityForItem
    - _Requirements: 12.3, 12.4_

  - [ ] 3.4 Create ReceiptRepository and CreditNoteRepository
    - Create ReceiptRepository with findBySaleId, findByReceiptNumber
    - Create CreditNoteRepository with findByReturnId
    - _Requirements: 14.1, 14.2, 14.3_

- [ ] 4. Implement external API clients with resilience patterns
  - [ ] 4.1 Create ProductApiClient with Resilience4j annotations
    - Implement searchProducts(name, barcode) with @CircuitBreaker and @Retry
    - Implement getProduct(productId) with fallback
    - Implement checkStock(productId) with fallback
    - Implement decrementStock(adjustments) with fallback
    - Implement incrementStock(adjustments) with fallback
    - Create fallback methods that throw ExternalServiceException
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [ ] 4.2 Create CustomerApiClient with Resilience4j annotations
    - Implement searchCustomers(name, documentNumber) with @CircuitBreaker and @Retry
    - Implement getCustomer(customerId) with fallback
    - Implement verifyCreditStatus(customerId) with fallback
    - Create fallback methods that throw ExternalServiceException
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [ ] 4.3 Create DTOs for external API responses
    - Create ProductSummary record: id, name, barcode, unitPrice, availableStock, category
    - Create ProductDetails record with full product information
    - Create StockInfo record: available, quantity
    - Create CustomerSummary record: id, fullName, documentType, documentNumber, creditStatus
    - Create CustomerDetails record with full customer information
    - Create StockAdjustment record: productId, quantity
    - _Requirements: 1.3, 2.3_

  - [ ]* 4.4 Write integration tests for external API clients
    - Test circuit breaker opens after failure threshold
    - Test retry behavior on transient failures
    - Test fallback methods return proper error responses
    - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5_

- [ ] 5. Implement domain components
  - [ ] 5.1 Implement SaleStateMachine
    - Define ALLOWED_TRANSITIONS map for state transitions
    - Implement canTransition(currentStatus, event) method
    - Implement validateTransition(currentStatus, event) that throws InvalidSaleStateException
    - Implement getTargetStatus(event) method
    - _Requirements: 9.1, 9.2, 10.1, 10.2, 10.4, 10.5, 11.1, 11.2, 12.1, 12.2_

  - [ ]* 5.2 Write property tests for SaleStateMachine
    - **Property 21: Cancellation State Validation** - Only ACTIVE and FROZEN can cancel
    - **Property 26: Freeze State Validation** - Only ACTIVE can freeze
    - **Property 28: Resume State Validation** - Only FROZEN can resume
    - **Property 30: Full Return State Validation** - Only COMPLETED can full return
    - **Property 33: Partial Return State Validation** - Only COMPLETED or PARTIALLY_RETURNED can partial return
    - **Validates: Requirements 9.1, 9.2, 10.1, 10.2, 10.4, 10.5, 11.1, 11.2, 12.1, 12.2**

  - [ ] 5.3 Implement TotalsCalculator with BigDecimal precision
    - Implement calculateLineTotal(unitPrice, quantity) with BigDecimal
    - Implement calculateSubtotal(items) - sum of line totals
    - Implement calculateTax(subtotal) - subtotal * taxRate
    - Implement calculateDiscount(subtotal, discount) - percentage or fixed
    - Implement calculateTotal(subtotal, tax, discount)
    - All calculations use scale 2 with HALF_UP rounding
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_

  - [ ]* 5.4 Write property tests for TotalsCalculator
    - **Property 7: Subtotal Calculation** - Subtotal equals sum of line totals
    - **Property 8: Tax Calculation** - Tax equals subtotal * tax rate
    - **Property 9: Discount Calculation** - Discount calculated correctly for percentage and fixed
    - **Property 10: Total Calculation** - Total equals subtotal + tax - discount
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7**

  - [ ] 5.5 Implement ReceiptGenerator
    - Implement generateCheckoutReceipt(sale, payment) with full receipt content
    - Implement generateReturnReceipt(sale, return) with return details
    - Implement generateCreditNote(sale, return) for credit returns
    - Generate unique receipt numbers and credit note numbers
    - _Requirements: 8.9, 11.4, 11.6, 12.9, 12.10, 14.1, 14.2, 14.3_

- [ ] 6. Implement service layer
  - [ ] 6.1 Implement ProductService
    - Implement searchProducts(name, barcode) that delegates to ProductApiClient
    - Handle ExternalServiceException and return 503 responses
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [ ] 6.2 Implement CustomerService
    - Implement searchCustomers(name, documentNumber) that delegates to CustomerApiClient
    - Implement getCustomer(customerId) with not found handling
    - Implement verifyCreditStatus(customerId) returning CreditStatus
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 7.3, 7.4_

  - [ ] 6.3 Implement SaleService - sale creation and retrieval
    - Implement createSale(command) that creates ACTIVE sale with zero totals
    - Implement getSale(saleId) with SaleNotFoundException handling
    - Validate customer exists if customerId provided
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 16.1, 16.2_

  - [ ]* 6.4 Write property tests for SaleService creation
    - **Property 1: Sale Initialization** - Created sale has ACTIVE status, zero totals, unique ID, timestamp
    - **Property 2: Customer Association** - Sale is associated with customer when provided
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**

  - [ ] 6.5 Implement SaleService - item management
    - Implement addItem(saleId, command) with product lookup, stock validation, duplicate consolidation
    - Implement updateItemQuantity(saleId, itemId, quantity) with quantity >= 1 validation
    - Implement removeItem(saleId, itemId) with totals recalculation
    - Validate sale is in ACTIVE status for all operations
    - Trigger totals recalculation after each operation
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8_

  - [ ]* 6.6 Write property tests for item management
    - **Property 3: Item Quantity Validation** - Reject quantity < 1
    - **Property 4: Duplicate Item Consolidation** - Same product increments quantity
    - **Property 5: Totals Recalculation After Item Changes** - Totals recalculated on item changes
    - **Property 6: Price Snapshot Preservation** - Unit price captured and preserved
    - **Validates: Requirements 4.2, 4.3, 4.4, 4.5, 4.6, 4.8_

  - [ ] 6.7 Implement SaleService - discount and customer association
    - Implement applyDiscount(saleId, command) with percentage or fixed amount
    - Implement associateCustomer(saleId, customerId) with customer validation
    - Recalculate totals after discount application
    - _Requirements: 5.3, 5.4, 7.1_

  - [ ] 6.8 Implement SaleService - cancellation
    - Implement cancelSale(saleId, command) with state validation
    - Validate cancellation reason length <= 255 characters
    - Record cancellation reason and timestamp
    - Do NOT modify stock levels
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7_

  - [ ]* 6.9 Write property tests for cancellation
    - **Property 21: Cancellation State Validation** - Only ACTIVE or FROZEN can cancel
    - **Property 22: Cancellation Reason Validation** - Reason max 255 characters
    - **Property 23: Cancellation Data Persistence** - Reason and timestamp recorded
    - **Property 24: Cancellation Stock Invariant** - Stock unchanged after cancellation
    - **Property 25: Cancelled Sale Immutability** - No modifications after cancellation
    - **Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7**

  - [ ] 6.10 Implement FreezeService
    - Implement freezeSale(saleId) with state validation
    - Implement resumeSale(saleId) with state validation
    - Implement getFrozenSalesByTerminal(terminalId)
    - Record frozenAt timestamp when freezing
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

  - [ ]* 6.11 Write property tests for freeze/resume
    - **Property 26: Freeze State Validation** - Only ACTIVE can freeze
    - **Property 27: Frozen Sale Data Preservation** - Items and totals retained
    - **Property 28: Resume State Validation** - Only FROZEN can resume
    - **Property 29: Frozen Sales Query** - Return only FROZEN sales for terminal
    - **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5, 10.6**

- [ ] 7. Implement PaymentService
  - [ ] 7.1 Implement checkout validation logic
    - Validate sale has at least one item
    - Validate sale is in ACTIVE status
    - Validate stock availability for all items via Product API
    - Return HTTP 409 with stock issues if insufficient
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [ ] 7.2 Implement cash checkout processing
    - Validate amountReceived >= total (reject with 400 if not)
    - Calculate change = amountReceived - total
    - Set status to COMPLETED
    - Generate unique transaction ID
    - Decrement stock via Product API
    - Generate receipt with all required fields
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 8.6, 8.7, 8.8, 8.9_

  - [ ]* 7.3 Write property tests for cash checkout
    - **Property 11: Cash Payment Amount Validation** - Reject amountReceived < total
    - **Property 12: Cash Change Calculation** - Change = amountReceived - total
    - **Property 13: Cash Checkout Customer Optional** - Customer not required for cash
    - **Property 17: Checkout State Transition** - Status becomes COMPLETED
    - **Property 18: Checkout Requires Items** - Reject sale with no items
    - **Property 19: Transaction ID Uniqueness** - Unique transaction IDs
    - **Property 20: Receipt Content Completeness** - Receipt has all required fields
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5, 8.1, 8.2, 8.6, 8.8, 8.9, 14.1**

  - [ ] 7.4 Implement credit checkout processing
    - Validate customer is associated (reject with 422 if not)
    - Verify customer credit status is APPROVED via Customer API
    - Reject with 422 if credit status not APPROVED, include status in response
    - Generate unique credit reference number
    - Set status to COMPLETED
    - Generate unique transaction ID
    - Decrement stock via Product API
    - Generate receipt with credit reference
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9_

  - [ ]* 7.5 Write property tests for credit checkout
    - **Property 14: Credit Sale Requires Customer** - Reject without customer
    - **Property 15: Credit Status Validation** - Reject if status not APPROVED
    - **Property 16: Credit Reference Uniqueness** - Unique credit references
    - **Property 17: Checkout State Transition** - Status becomes COMPLETED
    - **Validates: Requirements 7.1, 7.2, 7.4, 7.5, 7.6, 8.6**

- [ ] 8. Implement ReturnService
  - [ ] 8.1 Implement full return processing
    - Validate sale status is COMPLETED
    - Set sale status to RETURNED
    - Require return reason
    - Generate credit note for credit sales
    - Increment stock via Product API
    - Generate return receipt with original transaction ID
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

  - [ ]* 8.2 Write property tests for full return
    - **Property 30: Full Return State Validation** - Only COMPLETED can full return
    - **Property 31: Full Return Credit Note** - Credit note for credit sales
    - **Property 32: Return Receipt Content** - Receipt references original transaction
    - **Validates: Requirements 11.1, 11.2, 11.4, 11.6, 14.2**

  - [ ] 8.3 Implement partial return processing
    - Validate sale status is COMPLETED or PARTIALLY_RETURNED
    - Validate return quantity <= purchased quantity - previously returned
    - Set status to PARTIALLY_RETURNED (or RETURNED if all items returned)
    - Require reason for each item
    - Generate credit note for credit sales
    - Increment stock only for returned items
    - Generate return receipt listing returned items only
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8, 12.9, 12.10_

  - [ ]* 8.4 Write property tests for partial return
    - **Property 33: Partial Return State Validation** - COMPLETED or PARTIALLY_RETURNED only
    - **Property 34: Return Quantity Validation** - Cannot exceed returnable quantity
    - **Property 35: Partial Return State Transition** - Status changes correctly
    - **Property 36: Partial Return Requires Reason Per Item** - Each item needs reason
    - **Property 37: Partial Return Credit Note** - Credit note for credit sales
    - **Property 38: RETURNED State Immutability** - No returns on RETURNED sales
    - **Property 39: PARTIALLY_RETURNED Additional Returns** - Allow additional returns
    - **Validates: Requirements 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8, 12.9, 12.10, 13.1, 13.2, 13.3**

- [ ] 9. Checkpoint - Core business logic complete
  - Ensure all property tests pass, ask the user if questions arise.

- [ ] 10. Implement controller layer
  - [ ] 10.1 Implement ProductController
    - Implement GET /api/v1/products/search with name and barcode params
    - Return List<ProductSummary> with HTTP 200
    - Handle ExternalServiceException with 503 response
    - Add OpenAPI annotations for documentation
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [ ] 10.2 Implement CustomerController
    - Implement GET /api/v1/customers/search with name and documentNumber params
    - Return List<CustomerSummary> with HTTP 200
    - Handle ExternalServiceException with 503 response
    - Add OpenAPI annotations for documentation
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [ ] 10.3 Implement SaleController - creation and retrieval
    - Implement POST /api/v1/sales with CreateSaleRequest
    - Implement GET /api/v1/sales/{saleId}
    - Add Jakarta Bean Validation annotations on request DTOs
    - Add OpenAPI annotations for documentation
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 16.1, 16.2_

  - [ ] 10.4 Implement SaleController - item management
    - Implement POST /api/v1/sales/{saleId}/items with AddItemRequest
    - Implement PUT /api/v1/sales/{saleId}/items/{itemId} with UpdateItemRequest
    - Implement DELETE /api/v1/sales/{saleId}/items/{itemId}
    - Add validation annotations for quantity >= 1
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8_

  - [ ] 10.5 Implement SaleController - discount and customer
    - Implement PUT /api/v1/sales/{saleId}/discount with DiscountRequest
    - Implement PUT /api/v1/sales/{saleId}/customer with CustomerAssociationRequest
    - _Requirements: 5.3, 5.4, 7.1_

  - [ ] 10.6 Implement SaleController - checkout and cancellation
    - Implement POST /api/v1/sales/{saleId}/checkout with CheckoutRequest
    - Implement POST /api/v1/sales/{saleId}/cancel with CancelRequest
    - Return CheckoutResponse with sale, receipt, and change
    - _Requirements: 6.1-6.5, 7.1-7.6, 8.1-8.9, 9.1-9.7_

  - [ ] 10.7 Implement SaleController - freeze and resume
    - Implement POST /api/v1/sales/{saleId}/freeze
    - Implement POST /api/v1/sales/{saleId}/resume
    - Implement GET /api/v1/sales/frozen with terminalId param
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

  - [ ] 10.8 Implement SaleController - returns
    - Implement POST /api/v1/sales/{saleId}/returns/full with FullReturnRequest
    - Implement POST /api/v1/sales/{saleId}/returns/partial with PartialReturnRequest
    - Return ReturnResponse with credit note and receipt
    - _Requirements: 11.1-11.6, 12.1-12.10, 13.1-13.3_

- [ ] 11. Implement error handling and validation
  - [ ] 11.1 Create exception hierarchy
    - Create SalesApiException base class with errorCode
    - Create SaleNotFoundException, InvalidSaleStateException
    - Create InsufficientStockException with List<StockIssue>
    - Create PaymentValidationException, CreditStatusException
    - Create ExternalServiceException with serviceName
    - Create ReturnValidationException
    - _Requirements: 9.1, 9.2, 8.5, 6.3, 7.2, 7.4, 15.1-15.5_

  - [ ] 11.2 Create error response DTOs
    - Create ErrorResponse record with timestamp, status, error, errorCode, message, path, fieldErrors, details
    - Create FieldError record with field, message, rejectedValue
    - Create StockIssue record with productId, productName, requestedQuantity, availableQuantity
    - _Requirements: Error Handling_

  - [ ] 11.3 Implement GlobalExceptionHandler
    - Handle SaleNotFoundException with 404 response
    - Handle InvalidSaleStateException with 400 response
    - Handle InsufficientStockException with 409 response including stock issues
    - Handle PaymentValidationException with 400 response
    - Handle CreditStatusException with 422 response including credit status
    - Handle ExternalServiceException with 503 response
    - Handle MethodArgumentNotValidException with 400 and field errors
    - Handle ReturnValidationException with 400 response
    - _Requirements: 9.2, 8.5, 6.3, 7.2, 7.4, 15.1-15.5_

  - [ ] 11.4 Create request DTOs with Jakarta validation
    - Create CreateSaleRequest with @NotBlank terminalId, cashierId
    - Create AddItemRequest with @NotNull productId, @Min(1) quantity
    - Create UpdateItemRequest with @Min(1) quantity
    - Create CheckoutRequest with @NotNull paymentType
    - Create CancelRequest with @NotBlank @Size(max=255) reason
    - Create DiscountRequest with percentage or fixedAmount
    - Create FullReturnRequest with @NotBlank reason
    - Create PartialReturnRequest with @NotEmpty items list
    - Create ReturnItemRequest with @NotNull saleItemId, @Min(1) quantity, @NotBlank reason
    - _Requirements: 4.2, 4.4, 8.3, 9.3, 9.4, 12.7_

- [ ] 12. Implement scheduled jobs
  - [ ] 12.1 Create FrozenSaleExpirationJob
    - Create @Scheduled method to run every minute
    - Query for sales with status FROZEN and frozenAt < threshold (configurable hours)
    - Cancel each expired sale with reason "Automatic cancellation due to expiration"
    - Log number of cancelled sales
    - _Requirements: 10.7, 10.8, 17.1, 17.2, 17.3_

  - [ ] 12.2 Configure scheduling
    - Enable scheduling with @EnableScheduling
    - Configure frozen sale expiration time via application property
    - Default expiration time: 2 hours
    - _Requirements: 10.7, 17.1, 18.2_

- [ ] 13. Checkpoint - All features implemented
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 14. Write integration tests
  - [ ]* 14.1 Write integration tests for product and customer search
    - Test successful product search by name and barcode
    - Test successful customer search by name and document number
    - Test 503 response when external APIs unavailable (using WireMock)
    - _Requirements: 1.1-1.5, 2.1-2.5, 15.1, 15.4_

  - [ ]* 14.2 Write integration tests for sale lifecycle
    - Test sale creation with and without customer
    - Test item addition, update, and removal
    - Test checkout with cash payment
    - Test checkout with credit payment
    - Test cancellation flow
    - Test freeze and resume
    - _Requirements: 3.1-3.5, 4.1-4.8, 6.1-6.5, 7.1-7.6, 9.1-9.7, 10.1-10.6_

  - [ ]* 14.3 Write integration tests for returns
    - Test full return for completed sale
    - Test partial return for completed sale
    - Test multiple partial returns
    - Test return validation (wrong status, quantity exceeded)
    - Test credit note generation for credit sales
    - _Requirements: 11.1-11.6, 12.1-12.10, 13.1-13.3_

  - [ ]* 14.4 Write integration tests for scheduled jobs
    - Test frozen sale expiration job cancels expired sales
    - Test frozen sale within expiration is not cancelled
    - _Requirements: 10.7, 10.8, 17.1, 17.2, 17.3_

- [ ] 15. API documentation and final verification
  - [ ] 15.1 Complete OpenAPI documentation
    - Add @Tag annotations to all controllers
    - Add @Operation, @ApiResponse annotations to all endpoints
    - Document all request/response schemas
    - Document error response schemas
    - _Requirements: API Documentation_

  - [ ] 15.2 Final verification and testing
    - Run all unit tests and property tests
    - Run all integration tests
    - Verify test coverage meets targets (Services 90%, Controllers 80%)
    - Verify Swagger UI accessible and complete
    - _Requirements: All_

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties defined in the design document
- Unit tests validate specific examples and edge cases
- Integration tests verify full stack behavior with mocked external APIs
- The implementation uses Java 17+ with Spring Boot 3.x
- BigDecimal is used for all monetary calculations with scale 2
- Optimistic locking via @Version prevents concurrent modification issues
- Resilience4j provides circuit breaker, retry, and timeout patterns for external API calls

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "1.4"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3", "2.4", "2.5"] },
    { "id": 2, "tasks": ["3.1", "3.2", "3.3", "3.4", "4.3"] },
    { "id": 3, "tasks": ["4.1", "4.2", "5.1", "5.3", "5.5"] },
    { "id": 4, "tasks": ["5.2", "5.4", "6.1", "6.2"] },
    { "id": 5, "tasks": ["6.3", "6.5", "6.7", "4.4"] },
    { "id": 6, "tasks": ["6.4", "6.6", "6.8", "6.10", "2.6"] },
    { "id": 7, "tasks": ["6.9", "6.11", "7.1", "7.2", "7.4"] },
    { "id": 8, "tasks": ["7.3", "7.5", "8.1", "8.3"] },
    { "id": 9, "tasks": ["8.2", "8.4", "10.1", "10.2"] },
    { "id": 10, "tasks": ["10.3", "10.4", "10.5", "10.6", "10.7", "10.8"] },
    { "id": 11, "tasks": ["11.1", "11.2", "11.3", "11.4", "12.1", "12.2"] },
    { "id": 12, "tasks": ["14.1", "14.2", "14.3", "14.4", "15.1", "15.2"] }
  ]
}
```
