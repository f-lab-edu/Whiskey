package com.whiskey.domain.review.dto;

import com.whiskey.domain.review.Review;
import java.time.LocalDateTime;

public record ReviewInfo(
    long id,
    long whiskeyId,
    long memberId,
    String memberName,
    int starRate,
    String content,
    LocalDateTime createAt
) {
    public static ReviewInfo from(Review review) {
        return new ReviewInfo(
            review.getId(),
            review.getWhiskey().getId(),
            review.getMember().getId(),
            review.getMember().getMemberName(),
            review.getStarRate(),
            review.getContent(),
            review.getCreateAt()
        );
    }
}