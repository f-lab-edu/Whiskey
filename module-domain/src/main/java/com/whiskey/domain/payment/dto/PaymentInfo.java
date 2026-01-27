package com.whiskey.domain.payment.dto;

import com.whiskey.domain.payment.Payment;

public record PaymentInfo(
    Long memberId,
    Long amount,
    String description
) {
    public static PaymentInfo from(Payment payment) {
        return new PaymentInfo(
            payment.getMember().getId(),
            payment.getAmount(),
            payment.getDescription()
        );
    }
}
