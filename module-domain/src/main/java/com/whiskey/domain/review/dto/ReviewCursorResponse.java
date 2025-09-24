package com.whiskey.domain.review.dto;

import java.util.List;

public record ReviewCursorResponse<T>(
    List<T> data,
    String nextCursor,
    boolean hasNext
) {
    public static <T> ReviewCursorResponse<T> of(List<T> data, String nextCursor, boolean hasNext) {
        return new ReviewCursorResponse<>(data, nextCursor, hasNext);
    }
}