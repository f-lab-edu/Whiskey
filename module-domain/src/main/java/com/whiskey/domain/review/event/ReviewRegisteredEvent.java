package com.whiskey.domain.review.event;

public record ReviewRegisteredEvent(
    long whiskeyId,
    long memberId,
    int starRate
) {}