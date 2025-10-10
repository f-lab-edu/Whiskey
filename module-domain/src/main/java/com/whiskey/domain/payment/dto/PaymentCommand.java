package com.whiskey.domain.payment.dto;

public record PaymentCommand(
    long memberId,
    String orderId,
    String description
) {}