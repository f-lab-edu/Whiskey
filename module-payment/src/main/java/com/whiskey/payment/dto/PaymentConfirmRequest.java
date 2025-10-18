package com.whiskey.payment.dto;

import lombok.Builder;

@Builder
public record PaymentConfirmRequest(
    String paymentKey,
    String orderId,
    Long amount
) {

    public static PaymentConfirmRequest of(String paymentKey, String orderId, Long amount) {
        return PaymentConfirmRequest.builder()
            .paymentKey(paymentKey)
            .orderId(orderId)
            .amount(amount)
            .build();
    }
}