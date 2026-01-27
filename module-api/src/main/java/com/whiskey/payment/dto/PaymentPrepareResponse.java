package com.whiskey.payment.dto;

import com.whiskey.domain.payment.dto.PaymentPrepareResult;
import lombok.Builder;

@Builder
public record PaymentPrepareResponse(
    String orderId,
    Long amount
) {
    public static PaymentPrepareResponse from(PaymentPrepareResult result) {
        return new PaymentPrepareResponse(result.orderId(), result.amount());
    }
}