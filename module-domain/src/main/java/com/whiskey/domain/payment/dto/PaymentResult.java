package com.whiskey.domain.payment.dto;

import java.math.BigDecimal;

public record PaymentResult(
    String orderId,
    BigDecimal amount
) {}