package com.supermarket.pos.salesapi.model.dto;

import com.supermarket.pos.salesapi.model.enums.CreditStatus;

/**
 * Summary of a customer returned by the Customer API.
 */
public record CustomerSummary(
    Long id,
    String fullName,
    String documentType,
    String documentNumber,
    CreditStatus creditStatus
) {}
