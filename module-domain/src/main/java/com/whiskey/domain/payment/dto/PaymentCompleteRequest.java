package com.whiskey.domain.payment.dto;

import com.whiskey.domain.payment.Payment;
import com.whiskey.payment.dto.PaymentResponse;

public record PaymentCompleteRequest(
    String paymentOrderId,
    String paymentKey,
    Long orderId,
    Long paymentId
) {
    public static PaymentCompleteRequest from(Payment payment, PaymentResponse paymentResponse) {
        return new PaymentCompleteRequest(
            payment.getPaymentOrderId(),
            paymentResponse.paymentKey(),
            payment.getOrder().getId(),
            payment.getId()
        );
    }

    public PaymentCompensationRequest toCompensationRequest() {
        return new PaymentCompensationRequest(
            this.paymentId,
            this.orderId,
            this.paymentOrderId,
            this.paymentKey
        );
    }
}