package com.whiskey.payment.dto;

public record PaymentConfirmRequest(
    String paymentKey,
    String orderId,
    Long amount
) {

    public static PaymentConfirmRequest of(String paymentKey, String orderId, Long amount) {
        return new PaymentConfirmRequest(paymentKey, orderId, amount);
    }
}