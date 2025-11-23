package com.whiskey.domain.payment.dto;

public record PaymentCompensationRequest(
    Long paymentId,
    Long orderId,
    String paymentOrderId,
    String paymentKey
) {}