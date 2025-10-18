package com.whiskey.payment.dto;

import jakarta.validation.constraints.NotNull;

public record PaymentPrepareRequest(
    @NotNull
    Long orderId,
    @NotNull
    Long amount,
    String description
) {}