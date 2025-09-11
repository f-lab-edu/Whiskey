package com.whiskey.domain.review.dto;

public record ReviewCursorRequest(
    String cursor,
    int size
) {
    public static ReviewCursorRequest of(String cursor, int size) {
        return new ReviewCursorRequest(cursor, size);
    }
}