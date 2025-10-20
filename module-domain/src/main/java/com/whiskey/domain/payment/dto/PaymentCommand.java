package com.whiskey.domain.payment.dto;

public record PaymentCommand(
    Long memberId,
    Long orderId,
    Long amount,
    String description
) {}