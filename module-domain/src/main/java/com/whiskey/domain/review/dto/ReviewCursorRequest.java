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
    // ID 커서 추출
    public Long getCursorId() {
        if(cursor == null) {
            return null;
        }

        if(sortType == ReviewSortType.RATING_HIGH || sortType == ReviewSortType.RATING_LOW) {
            String[] cursorParts = cursor.split("_");
            return Long.parseLong(cursorParts[1]);
        }

        return Long.parseLong(cursor);
    }

    // 평점 커서 추출
    public Integer getCursorRating() {
        if(cursor == null || sortType == ReviewSortType.LATEST) {
            return null;
        }

        String[] cursorParts = cursor.split("_");
        return Integer.parseInt(cursorParts[0]);
    }
}