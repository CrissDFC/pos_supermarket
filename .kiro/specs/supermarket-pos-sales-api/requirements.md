# Requirements Document

## Introduction

This document defines the requirements for the Sales REST API of a supermarket Point of Sale (POS) system. The Sales API manages the complete lifecycle of sales transactions, including creation, modification, payment processing, freezing, cancellation, and returns. The API integrates with two existing external services: the Product API for product catalog and stock management, and the Customer API for customer information and credit status verification.

## Glossary

- **Sales_API**: The REST API system being developed that handles all sales-related operations for the supermarket POS terminals
- **Product_API**: External service that manages the product catalog, pricing, and stock levels
- **Customer_API**: External service that manages customer records, including personal information and credit status
- **Sale**: A transaction entity representing a purchase at a POS terminal, containing items, totals, and payment information
- **Sale_Item**: A line item within a sale representing a specific product with quantity and price
- **POS_Terminal**: The physical checkout station where sales transactions are processed
- **Cashier**: The employee operating the POS terminal, identified by a unique ID
- **Customer**: A person who may be associated with a sale for loyalty or credit purposes
- **Credit_Status**: The approval state of a customer's credit eligibility (APPROVED, REJECTED, PENDING)
- **Sale_Status**: The current state of a sale in its lifecycle (ACTIVE, COMPLETED, CANCELLED, FROZEN, RETURNED, PARTIALLY_RETURNED)
- **Payment_Type**: The method of payment for a sale (CASH, CREDIT)
- **Receipt**: A document generated upon successful checkout containing transaction details
- **Credit_Note**: A document generated for returns on credit sales instead of cash refund
- **Frozen_Sale**: A sale that has been temporarily paused, allowing the cashier to attend another customer
- **Return**: The process of accepting back items from a completed sale
- **Transaction_ID**: A unique identifier generated for each completed sale

## Requirements

### Requirement 1: Product Search

**User Story:** As a cashier, I want to search for products by name or barcode, so that I can quickly find items to add to a sale.

#### Acceptance Criteria

1. WHEN a product search request contains a product name, THE Sales_API SHALL query the Product_API for products with names matching the search term (partial match, case-insensitive)
2. WHEN a product search request contains a barcode, THE Sales_API SHALL query the Product_API for the product with an exact barcode match
3. WHEN the Product_API returns product results, THE Sales_API SHALL return a list containing product ID, name, barcode, unit price, available stock, and category for each matching product
4. WHEN the Product_API is unavailable, THE Sales_API SHALL return a 503 Service Unavailable response with a descriptive error message
5. WHEN no products match the search criteria, THE Sales_API SHALL return an empty list with HTTP 200 status

### Requirement 2: Customer Search

**User Story:** As a cashier, I want to search for customers by name or document number, so that I can associate them with sales or verify their credit status.

#### Acceptance Criteria

1. WHEN a customer search request contains a customer name, THE Sales_API SHALL query the Customer_API for customers with names matching the search term (partial match)
2. WHEN a customer search request contains a document number, THE Sales_API SHALL query the Customer_API for the customer with an exact document number match
3. WHEN the Customer_API returns customer results, THE Sales_API SHALL return a list containing customer ID, full name, document type, document number, and credit status (APPROVED, REJECTED, PENDING) for each matching customer
4. WHEN the Customer_API is unavailable, THE Sales_API SHALL return a 503 Service Unavailable response with a descriptive error message
5. WHEN no customers match the search criteria, THE Sales_API SHALL return an empty list with HTTP 200 status

### Requirement 3: Sale Creation

**User Story:** As a cashier, I want to create a new sale transaction, so that I can begin processing a customer's purchase.

#### Acceptance Criteria

1. WHEN a sale creation request is received with a POS terminal ID and cashier ID, THE Sales_API SHALL create a new sale with status ACTIVE
2. WHEN a sale creation request includes a customer ID, THE Sales_API SHALL associate the customer with the sale
3. WHEN a sale is created, THE Sales_API SHALL initialize the subtotal, tax, discount, and total to zero
4. WHEN a sale is created, THE Sales_API SHALL assign a unique sale identifier
5. WHEN a sale is created, THE Sales_API SHALL record the creation timestamp

### Requirement 4: Sale Item Management

**User Story:** As a cashier, I want to add, update, and remove items from a sale, so that I can build the customer's shopping cart.

#### Acceptance Criteria

1. WHEN an item is added to an ACTIVE sale by product ID or barcode, THE Sales_API SHALL retrieve the product details from the Product_API and create a Sale_Item with product ID, product name, unit price (snapshot), quantity, and line total
2. WHEN an item is added with quantity less than 1, THE Sales_API SHALL reject the request with HTTP 400 Bad Request
3. WHEN an item is added to a sale where the same product already exists, THE Sales_API SHALL increment the existing item's quantity by the new quantity
4. WHEN an item quantity is updated, THE Sales_API SHALL validate the new quantity is at least 1
5. WHEN an item is removed from a sale, THE Sales_API SHALL delete the Sale_Item and recalculate sale totals
6. WHEN any item is added, updated, or removed, THE Sales_API SHALL recalculate the sale subtotal, tax, and total
7. WHEN adding or updating an item quantity, THE Sales_API SHALL validate that the requested quantity does not exceed available stock by querying the Product_API, returning HTTP 409 Conflict if insufficient stock exists
8. WHEN an item is added, THE Sales_API SHALL capture the unit price as a snapshot to preserve the price at time of addition

### Requirement 5: Sale Totals Calculation

**User Story:** As a system, I want to calculate sale totals accurately, so that customers are charged the correct amount.

#### Acceptance Criteria

1. WHEN sale totals are calculated, THE Sales_API SHALL compute the subtotal as the sum of all line totals (unit price × quantity for each item)
2. WHEN sale totals are calculated, THE Sales_API SHALL compute the tax as subtotal multiplied by the tax rate (configurable, default 19%)
3. WHEN a discount is applied as a percentage, THE Sales_API SHALL compute the discount amount as subtotal multiplied by the discount percentage
4. WHEN a discount is applied as a fixed amount, THE Sales_API SHALL use the fixed amount as the discount value
5. WHEN sale totals are calculated, THE Sales_API SHALL compute the total as subtotal plus tax minus discount
6. WHEN performing monetary calculations, THE Sales_API SHALL use BigDecimal with 2 decimal precision
7. WHEN performing monetary calculations internally, THE Sales_API SHALL use integer arithmetic in cents to avoid floating-point errors

### Requirement 6: Cash Sale Payment

**User Story:** As a cashier, I want to process cash payments, so that customers can pay with physical currency.

#### Acceptance Criteria

1. WHEN a cash sale checkout is initiated, THE Sales_API SHALL validate that a customer association is optional
2. WHEN a cash sale checkout is initiated, THE Sales_API SHALL validate that the amount received is greater than or equal to the sale total
3. WHEN the amount received is less than the sale total, THE Sales_API SHALL reject the checkout with HTTP 400 Bad Request
4. WHEN a cash sale checkout succeeds, THE Sales_API SHALL calculate the change as amount received minus total
5. WHEN a cash sale checkout succeeds, THE Sales_API SHALL set the sale status to COMPLETED

### Requirement 7: Credit Sale Payment

**User Story:** As a cashier, I want to process credit sales, so that approved customers can purchase on credit.

#### Acceptance Criteria

1. WHEN a credit sale checkout is initiated, THE Sales_API SHALL validate that a customer is associated with the sale
2. WHEN a credit sale checkout is initiated without a customer, THE Sales_API SHALL reject the checkout with HTTP 422 Unprocessable Entity
3. WHEN a credit sale checkout is initiated, THE Sales_API SHALL query the Customer_API to verify the customer's credit status is APPROVED
4. WHEN the customer's credit status is not APPROVED (REJECTED or PENDING), THE Sales_API SHALL reject the checkout with HTTP 422 Unprocessable Entity and include the credit status in the error response
5. WHEN a credit sale checkout succeeds, THE Sales_API SHALL generate a unique credit reference number
6. WHEN a credit sale checkout succeeds, THE Sales_API SHALL set the sale status to COMPLETED

### Requirement 8: Sale Checkout

**User Story:** As a cashier, I want to complete a sale checkout, so that the transaction is finalized and a receipt is generated.

#### Acceptance Criteria

1. WHEN a checkout is initiated, THE Sales_API SHALL validate that the sale has at least one item
2. WHEN a checkout is initiated for a sale with no items, THE Sales_API SHALL reject the request with HTTP 400 Bad Request
3. WHEN a checkout is initiated, THE Sales_API SHALL validate that payment information is complete (payment type and required payment details)
4. WHEN a checkout is initiated, THE Sales_API SHALL validate stock availability for all items by querying the Product_API
5. WHEN any item has insufficient stock at checkout time, THE Sales_API SHALL reject the checkout with HTTP 409 Conflict and include the list of out-of-stock items with their requested and available quantities
6. WHEN a checkout succeeds, THE Sales_API SHALL set the sale status to COMPLETED
7. WHEN a checkout succeeds, THE Sales_API SHALL decrement stock for all items via the Product_API
8. WHEN a checkout succeeds, THE Sales_API SHALL generate a unique transaction ID
9. WHEN a checkout succeeds, THE Sales_API SHALL generate a receipt containing store name, terminal ID, cashier ID, date/time, customer info (if present), items with prices, subtotal, tax, discount, total, payment method, amount received (cash), change (cash), and transaction ID

### Requirement 9: Sale Cancellation

**User Story:** As a cashier, I want to cancel an active or frozen sale, so that I can abandon a transaction that will not be completed.

#### Acceptance Criteria

1. WHEN a cancellation request is received for a sale with status ACTIVE or FROZEN, THE Sales_API SHALL set the sale status to CANCELLED
2. WHEN a cancellation request is received for a sale with status other than ACTIVE or FROZEN, THE Sales_API SHALL reject the request with HTTP 400 Bad Request
3. WHEN a cancellation request is received, THE Sales_API SHALL require a cancellation reason with maximum 255 characters
4. WHEN a cancellation reason exceeds 255 characters, THE Sales_API SHALL reject the request with HTTP 400 Bad Request
5. WHEN a sale is cancelled, THE Sales_API SHALL record the cancellation reason and cancellation timestamp
6. WHEN a sale is cancelled, THE Sales_API SHALL NOT modify stock levels
7. WHEN a sale is cancelled, THE Sales_API SHALL prevent any further modifications or checkout attempts, returning HTTP 400 Bad Request for any such attempts

### Requirement 10: Sale Freezing

**User Story:** As a cashier, I want to freeze an active sale, so that I can temporarily pause it to attend another customer.

#### Acceptance Criteria

1. WHEN a freeze request is received for a sale with status ACTIVE, THE Sales_API SHALL set the sale status to FROZEN
2. WHEN a freeze request is received for a sale with status other than ACTIVE, THE Sales_API SHALL reject the request with HTTP 400 Bad Request
3. WHEN a sale is frozen, THE Sales_API SHALL retain all items and calculated totals
4. WHEN a resume request is received for a sale with status FROZEN, THE Sales_API SHALL set the sale status back to ACTIVE
5. WHEN a resume request is received for a sale with status other than FROZEN, THE Sales_API SHALL reject the request with HTTP 400 Bad Request
6. WHEN a request is made to list frozen sales by POS terminal ID, THE Sales_API SHALL return all sales with status FROZEN for that terminal
7. WHEN a sale remains in FROZEN status for longer than the configurable expiration time (default 2 hours), THE Sales_API SHALL automatically cancel the sale
8. WHEN a frozen sale is automatically cancelled due to expiration, THE Sales_API SHALL record "Automatic cancellation due to expiration" as the cancellation reason

### Requirement 11: Full Return

**User Story:** As a cashier, I want to process a full return for a completed sale, so that a customer can return all purchased items.

#### Acceptance Criteria

1. WHEN a full return request is received for a sale with status COMPLETED, THE Sales_API SHALL set the sale status to RETURNED
2. WHEN a full return request is received for a sale with status other than COMPLETED, THE Sales_API SHALL reject the request with HTTP 400 Bad Request
3. WHEN a full return is processed, THE Sales_API SHALL require a return reason
4. WHEN a full return is processed for a credit sale, THE Sales_API SHALL generate a credit note instead of calculating a cash refund
5. WHEN a full return is processed, THE Sales_API SHALL increment stock for all items via the Product_API
6. WHEN a full return is processed, THE Sales_API SHALL generate a return receipt referencing the original transaction ID and listing all returned items

### Requirement 12: Partial Return

**User Story:** As a cashier, I want to process a partial return for specific items, so that a customer can return only some items from a purchase.

#### Acceptance Criteria

1. WHEN a partial return request is received for a sale with status COMPLETED or PARTIALLY_RETURNED, THE Sales_API SHALL allow the return of specified items and quantities
2. WHEN a partial return request is received for a sale with status other than COMPLETED or PARTIALLY_RETURNED, THE Sales_API SHALL reject the request with HTTP 400 Bad Request
3. WHEN a partial return is processed, THE Sales_API SHALL validate that the return quantity for each item does not exceed the originally purchased quantity minus any previously returned quantity
4. WHEN a return quantity exceeds the available quantity for return, THE Sales_API SHALL reject the request with HTTP 400 Bad Request and include the item details and maximum returnable quantity
5. WHEN a partial return is processed, THE Sales_API SHALL set the sale status to PARTIALLY_RETURNED
6. WHEN a partial return includes all remaining items in the sale, THE Sales_API SHALL set the sale status to RETURNED
7. WHEN a partial return is processed, THE Sales_API SHALL require a return reason for each item
8. WHEN a partial return is processed, THE Sales_API SHALL increment stock only for the returned items via the Product_API
9. WHEN a partial return is processed for a credit sale, THE Sales_API SHALL generate a credit note for the returned amount
10. WHEN a partial return is processed, THE Sales_API SHALL generate a return receipt listing only the returned items and referencing the original transaction ID

### Requirement 13: Return Constraints

**User Story:** As a system, I want to enforce return constraints, so that returns are processed fairly and accurately.

#### Acceptance Criteria

1. WHEN a return is attempted on a sale that has already been fully returned (status RETURNED), THE Sales_API SHALL reject the request with HTTP 400 Bad Request
2. WHEN a return is attempted on a sale with status PARTIALLY_RETURNED, THE Sales_API SHALL allow additional partial returns for items that have remaining returnable quantity
3. WHEN a sale status is RETURNED, THE Sales_API SHALL prevent any further return attempts, returning HTTP 400 Bad Request

### Requirement 14: Receipt Generation

**User Story:** As a system, I want to generate receipts automatically, so that customers have proof of their transactions.

#### Acceptance Criteria

1. WHEN a checkout completes successfully, THE Sales_API SHALL generate a receipt containing store name, terminal ID, cashier ID, date/time, customer information (if a customer is associated with the sale), item list with product names, quantities, unit prices, and line totals, subtotal, tax amount and rate, discount amount (if applied), total, payment method, amount received (for cash payments), change amount (for cash payments), credit reference number (for credit payments), and transaction ID
2. WHEN a return is processed, THE Sales_API SHALL generate a return receipt containing the original transaction ID, return date/time, returned items with quantities and amounts, and credit note reference (for credit sales)
3. WHEN a receipt is generated, THE Sales_API SHALL assign a unique receipt number

### Requirement 15: Error Handling for External Services

**User Story:** As a system, I want to handle external service failures gracefully, so that users receive clear feedback when dependent services are unavailable.

#### Acceptance Criteria

1. WHEN the Product_API is unavailable during a product search, THE Sales_API SHALL return HTTP 503 Service Unavailable with an error message indicating the Product_API is temporarily unavailable
2. WHEN the Product_API is unavailable during stock validation, THE Sales_API SHALL return HTTP 503 Service Unavailable with an error message indicating the inability to validate stock
3. WHEN the Product_API is unavailable during checkout stock decrement, THE Sales_API SHALL return HTTP 503 Service Unavailable and NOT change the sale status
4. WHEN the Customer_API is unavailable during a customer search, THE Sales_API SHALL return HTTP 503 Service Unavailable with an error message indicating the Customer_API is temporarily unavailable
5. WHEN the Customer_API is unavailable during credit status verification, THE Sales_API SHALL return HTTP 503 Service Unavailable with an error message indicating the inability to verify credit status

### Requirement 16: Sale Retrieval

**User Story:** As a cashier, I want to retrieve sale details, so that I can view the current state of a transaction.

#### Acceptance Criteria

1. WHEN a sale retrieval request is received with a valid sale ID, THE Sales_API SHALL return the sale details including sale ID, status, terminal ID, cashier ID, customer information (if associated), items, subtotal, tax, discount, total, payment information, transaction ID (if completed), and timestamps
2. WHEN a sale retrieval request is received with an invalid sale ID, THE Sales_API SHALL return HTTP 404 Not Found

### Requirement 17: Frozen Sale Expiration Job

**User Story:** As a system, I want to automatically cancel expired frozen sales, so that abandoned transactions do not remain indefinitely.

#### Acceptance Criteria

1. WHEN the expiration job runs, THE Sales_API SHALL identify all sales with status FROZEN where the frozen timestamp is older than the configurable expiration threshold
2. WHEN expired frozen sales are identified, THE Sales_API SHALL cancel each sale with the reason "Automatic cancellation due to expiration"
3. WHEN the expiration job completes, THE Sales_API SHALL log the number of cancelled sales

### Requirement 18: Configuration

**User Story:** As a system administrator, I want to configure system parameters, so that the API can adapt to different business requirements.

#### Acceptance Criteria

1. WHERE the tax rate configuration is set, THE Sales_API SHALL use the configured value for tax calculations, defaulting to 19% if not specified
2. WHERE the frozen sale expiration time is set, THE Sales_API SHALL use the configured value for automatic cancellation, defaulting to 2 hours if not specified
3. WHERE the store name configuration is set, THE Sales_API SHALL use the configured value on receipts
