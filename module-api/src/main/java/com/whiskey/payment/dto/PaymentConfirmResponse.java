package com.whiskey.payment.dto;

import com.whiskey.domain.payment.Payment;
import com.whiskey.domain.payment.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PaymentConfirmResponse(
    String paymentKey,
    String paymentOrderId,
    BigDecimal totalAmount,
    PaymentStatus paymentStatus,
    LocalDateTime approvedAt
) {
    public static PaymentConfirmResponse from(Payment payment) {
        return PaymentConfirmResponse.builder()
            .paymentKey(payment.getPaymentKey())
            .paymentOrderId(payment.getPaymentOrderId())
            .totalAmount(payment.getAmount())
            .paymentStatus(payment.getPaymentStatus())
            .approvedAt(LocalDateTime.now())
            .build();
    }
}