package com.whiskey.domain.payment.dto;

public record PaymentPrepareResult(
    String orderId,
    Long amount
) {}