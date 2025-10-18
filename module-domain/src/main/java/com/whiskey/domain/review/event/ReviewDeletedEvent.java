package com.whiskey.domain.review.event;

public record ReviewDeletedEvent(
    long whiskeyId,
    long memberId
) {}