package com.whiskey.domain.review.repository;

import com.whiskey.domain.review.Review;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewRepositoryCustom {
    Page<Review> reviews(long whiskeyId, Pageable pageable);

    List<Review> findLatestReviews(long whiskeyId, Long cursorId, int size);
}