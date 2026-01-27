package com.whiskey.domain.payment.dto;

public record PaymentConfirmCommand(
    Long memberId,
    String orderId,
    Long amount,
    String paymentKey
) {}