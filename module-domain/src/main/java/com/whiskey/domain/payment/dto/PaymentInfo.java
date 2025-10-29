package com.whiskey.domain.payment.dto;

import com.whiskey.domain.payment.Payment;
import java.math.BigDecimal;

public record PaymentInfo(
    long memberId,
    BigDecimal amount,
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
