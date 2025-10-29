package com.whiskey.domain.review.event;

public record ReviewUpdatedEvent(
    long whiskeyId,
    long memberId,
    int oldRating,
    int newRating
) {}