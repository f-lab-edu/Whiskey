package com.whiskey.payment.dto;

import com.whiskey.payment.enums.TossPaymentStatus;

public record PaymentStatusResponse(
    String status
) {
    public TossPaymentStatus getPaymentStatus() {
        return TossPaymentStatus.valueOf(status);
    }

    public boolean isApproved() {
        return getPaymentStatus().isApproved();
    }
}