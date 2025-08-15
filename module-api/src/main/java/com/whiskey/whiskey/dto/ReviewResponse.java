package com.whiskey.whiskey.dto;

import com.whiskey.domain.review.Review;
import com.whiskey.domain.review.dto.ReviewInfo;
import java.time.LocalDateTime;

public record ReviewResponse(
    long id,
    long whiskey_id,
    long member_id,
    String memberName,
    int starRate,
    String content,
    LocalDateTime createdAt
) {
    public static ReviewResponse from(ReviewInfo reviewInfo) {
        return new ReviewResponse(
            reviewInfo.id(),
            reviewInfo.whiskeyId(),
            reviewInfo.memberId(),
            reviewInfo.memberName(),
            reviewInfo.starRate(),
            reviewInfo.content(),
            reviewInfo.createAt()
        );
    }
}