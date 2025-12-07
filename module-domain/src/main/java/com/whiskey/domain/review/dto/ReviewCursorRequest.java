package com.whiskey.domain.review.dto;

import com.whiskey.domain.review.enums.ReviewFilter;
import com.whiskey.domain.review.enums.ReviewSortType;

public record ReviewCursorRequest(
    Long whiskeyId,
    String cursor,
    int size,
    ReviewFilter filter,
    ReviewSortType sortType
) {
    // 최신순
    public Long getCursorId() {
        return cursor != null ? Long.parseLong(cursor) : null;
    }

    // 평점순
    public Integer getRatingCursorId() {
        if(cursor != null || sortType == ReviewSortType.LATEST) {
            return null;
        }

        String[] cursorParts = cursor.split("_");
        return Integer.parseInt(cursorParts[0]);
    }
}