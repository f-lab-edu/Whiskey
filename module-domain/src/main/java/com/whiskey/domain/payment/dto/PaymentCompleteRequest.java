package com.whiskey.domain.payment.dto;

import com.whiskey.domain.order.Order;
import com.whiskey.domain.payment.Payment;
import com.whiskey.payment.dto.PaymentResponse;

public record PaymentCompleteRequest(
    String paymentOrderId,
    String paymentKey,
    Long orderId
) {
    public static PaymentCompleteRequest from(Payment payment, Order order, PaymentResponse paymentResponse) {
        return new PaymentCompleteRequest(
            payment.getPaymentOrderId(),
            paymentResponse.paymentKey(),
            order.getId()
        );
    }

    public PaymentCompensationRequest toCompensationRequest(Long paymentId, Long orderId) {
        return new PaymentCompensationRequest(
            paymentId,
            orderId,
            paymentOrderId,
            paymentKey
        );
    }
}