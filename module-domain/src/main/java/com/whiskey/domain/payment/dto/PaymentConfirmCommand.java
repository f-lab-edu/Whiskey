package com.whiskey.domain.payment.dto;

import java.math.BigDecimal;

public record PaymentConfirmCommand(
    Long memberId,
    String orderId,
    BigDecimal amount,
    String paymentKey
) {}