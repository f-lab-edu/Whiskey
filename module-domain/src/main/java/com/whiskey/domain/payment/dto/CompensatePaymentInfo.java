package com.whiskey.domain.payment.dto;

import java.time.LocalDateTime;

public record CompensatePaymentInfo(
    Long paymentId,
    Long orderId,
    String paymentOrderId,
    String paymentKey,
    String reason,
    String errorMessage,
    LocalDateTime failedAt,
    int retryCount
) {
    public CompensatePaymentInfo incrRetryCount() {
        return new CompensatePaymentInfo(
            this.paymentId,
            this.orderId,
            this.paymentOrderId,
            this.paymentKey,
            this.reason,
            this.errorMessage,
            this.failedAt,
            this.retryCount + 1
        );
    }
}