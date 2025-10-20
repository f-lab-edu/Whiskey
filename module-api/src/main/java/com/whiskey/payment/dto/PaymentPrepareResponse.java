package com.whiskey.payment.dto;

import lombok.Builder;

@Builder
public record PaymentPrepareResponse(
    String orderId,
    String paymentKey,
    Long amount,
    String description
) {
    public static PaymentPrepareResponse of(String orderId, String paymentKey, Long amount, String description) {
        return new PaymentPrepareResponse(orderId, paymentKey, amount, description);
    }
}