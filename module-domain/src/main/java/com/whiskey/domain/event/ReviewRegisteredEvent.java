package com.whiskey.domain.event;

public record ReviewRegisteredEvent(
    long whiskeyId,
    long memberId,
    int starRate
) {}