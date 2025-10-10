package com.whiskey.payment.dto;

public record TossPaymentErrorResponse(
    String code,
    String message
) {}