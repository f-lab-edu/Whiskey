package com.whiskey.domain.review.repository;

import com.whiskey.domain.review.Review;
import com.whiskey.domain.review.dto.ReviewCursorRequest;
import java.util.List;

public interface ReviewRepositoryCustom {

    List<Review> findLatestReviews(ReviewCursorRequest request);

    List<Review> findByHighestRating(ReviewCursorRequest request);

    List<Review> findByLowestRating(ReviewCursorRequest request);
}