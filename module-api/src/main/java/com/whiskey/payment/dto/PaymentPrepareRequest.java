package com.whiskey.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentPrepareRequest(
    @NotNull
    Long orderId,
    @NotNull @Positive
    Long amount,
    String description
) {}