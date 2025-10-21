package com.whiskey.payment.dto;

import com.whiskey.domain.payment.dto.PaymentResult;
import lombok.Builder;

@Builder
public record PaymentPrepareResponse(
    String orderId,
    Long amount
) {
    public static PaymentPrepareResponse from(PaymentResult result) {
        return new PaymentPrepareResponse(result.orderId(), result.amount().longValueExact());
    }
}