package com.whiskey.domain.event;

public record ReviewUpdatedEvent(
    long whiskeyId,
    long memberId,
    int oldRating,
    int newRating
) {}