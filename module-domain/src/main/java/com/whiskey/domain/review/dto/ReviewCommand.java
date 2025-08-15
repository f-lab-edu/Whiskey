package com.whiskey.domain.review.dto;

public record ReviewCommand(
    Long whiskeyId,
    Long memberId,
    Integer starRate,
    String content
) {}