package com.whiskey.domain.payment.dto;

public record PaymentPrepareCommand(
    Long memberId,
    Long orderId,
    Long amount,
    String description
) {}