package com.whiskey.payment.dto;

public record PaymentResponse(
    String paymentKey,
    String orderId,
    Long amount
) {}