package com.whiskey.payment.dto;

import com.whiskey.domain.payment.Payment;
import com.whiskey.domain.payment.enums.PaymentStatus;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PaymentConfirmResponse(
    String paymentKey,
    String paymentOrderId,
    Long totalAmount,
    PaymentStatus status,
    LocalDateTime approvedAt
) {
    public static PaymentConfirmResponse from(Payment payment) {
        return PaymentConfirmResponse.builder()
            .paymentKey(payment.getPaymentKey())
            .paymentOrderId(payment.getPaymentOrderId())
            .totalAmount(payment.getAmount())
            .status(payment.getStatus())
            .approvedAt(LocalDateTime.now())
            .build();
    }
}