package com.whiskey.payment.dto;

import com.whiskey.payment.enums.TossPaymentStatus;

public record PaymentCancelResponse(
    String status
) {
    public TossPaymentStatus getPaymentStatus() {
        return TossPaymentStatus.valueOf(status);
    }

    public boolean isCanceled() {
        return getPaymentStatus().isCanceled();
    }
}