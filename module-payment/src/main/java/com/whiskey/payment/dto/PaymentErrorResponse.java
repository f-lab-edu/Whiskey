package com.whiskey.payment.dto;

public record PaymentErrorResponse(
    String code,
    String message
) {}