package com.whiskey.payment.dto;

import lombok.Builder;

@Builder
public record TossPaymentConfirmRequest(
    String paymentKey,
    String orderId,
    Long amount
) {

    public static TossPaymentConfirmRequest of(String paymentKey, String orderId, Long amount) {
        return TossPaymentConfirmRequest.builder()
            .paymentKey(paymentKey)
            .orderId(orderId)
            .amount(amount)
            .build();
    }
}