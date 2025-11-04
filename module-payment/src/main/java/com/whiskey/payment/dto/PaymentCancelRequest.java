package com.whiskey.payment.dto;

public record PaymentCancelRequest(
    String paymentKey,
    String orderId,
    String cancelReason
) {
    public static PaymentCancelRequest of(String paymentKey, String orderId, String cancelReason) {
        return new PaymentCancelRequest(paymentKey, orderId, cancelReason);
    }
}