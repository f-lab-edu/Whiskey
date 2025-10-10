package com.whiskey.domain.payment.dto;

import com.whiskey.domain.payment.Payment;

public record PaymentInfo(
    long memberId,
    long amount,
    String description
) {
    public static PaymentInfo from(Payme


        nt payment) {
        return new PaymentInfo(
            payment.getMember(),
            payment.amount(),
            payment.description()
        );
    }
}
