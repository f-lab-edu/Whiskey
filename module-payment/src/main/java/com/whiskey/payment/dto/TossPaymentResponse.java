package com.whiskey.payment.dto;

public record TossPaymentResponse(
    String paymentKey,
    String orderId,
    Long amount
) {}