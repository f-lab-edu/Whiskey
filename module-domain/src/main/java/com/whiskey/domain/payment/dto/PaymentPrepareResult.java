package com.whiskey.domain.payment.dto;

import java.math.BigDecimal;

public record PaymentPrepareResult(
    String orderId,
    BigDecimal amount
) {}