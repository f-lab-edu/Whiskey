package com.whiskey.domain.payment.dto;

import java.math.BigDecimal;

public record PaymentCommand(
    Long memberId,
    Long orderId,
    BigDecimal amount,
    String description
) {}