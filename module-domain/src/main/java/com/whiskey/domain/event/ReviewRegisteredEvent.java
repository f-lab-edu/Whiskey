package com.whiskey.domain.event;

public record ReviewRegisteredEvent(
    long whiskeyId,
    int starRate
) {}