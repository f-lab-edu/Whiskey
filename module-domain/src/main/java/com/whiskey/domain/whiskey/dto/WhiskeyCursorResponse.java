package com.whiskey.domain.whiskey.dto;

import java.util.List;

public record WhiskeyCursorResponse<T>(
    List<T> data,
    String nextCursor,
    boolean hasNext
) {
    public static <T> WhiskeyCursorResponse<T> of(List<T> data, String nextCursor, boolean hasNext) {
        return new WhiskeyCursorResponse<>(data, nextCursor, hasNext);
    }
}