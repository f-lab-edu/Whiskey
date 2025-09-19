package com.whiskey.domain.event;

public record ReviewDeletedEvent(
    long whiskeyId,
    long memberId
) {}