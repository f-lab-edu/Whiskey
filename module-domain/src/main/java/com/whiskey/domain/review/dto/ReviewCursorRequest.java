package com.whiskey.domain.review.dto;

import com.whiskey.domain.review.enums.ReviewFilter;

public record ReviewCursorRequest(
    String cursor,
    int size,
    ReviewFilter filter
) {
    public static ReviewCursorRequest of(String cursor, int size, ReviewFilter filter) {
        return new ReviewCursorRequest(cursor, size, filter);
    }
}