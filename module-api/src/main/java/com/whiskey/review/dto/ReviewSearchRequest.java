package com.whiskey.review.dto;

import com.whiskey.domain.review.dto.ReviewCursorRequest;
import com.whiskey.domain.review.enums.ReviewFilter;
import com.whiskey.domain.review.enums.ReviewSortType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ReviewSearchRequest(
    String cursor,
    @Min(7) @Max(50)
    Integer size,
    ReviewFilter filter,
    ReviewSortType sortType
) {
    public ReviewSearchRequest {
        if(size == null || size < 0) {
            size = 7;
        }

        if(filter == null) {
            filter = ReviewFilter.ACTIVE;
        }

        if(sortType == null) {
            sortType = ReviewSortType.LATEST;
        }
    }

    public ReviewCursorRequest toReviewCursorRequest(Long id) {
        return new ReviewCursorRequest(id, cursor, size, filter, sortType);
    }
}