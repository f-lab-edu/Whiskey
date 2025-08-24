package com.whiskey.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewRegisterRequest(
    @NotNull Long whiskeyId,
    @NotNull @Min(1) @Max(5) Integer starRate,
    String content
) {}