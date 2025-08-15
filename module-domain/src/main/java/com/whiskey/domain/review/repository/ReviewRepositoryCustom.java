package com.whiskey.domain.review.repository;

import com.whiskey.domain.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewRepositoryCustom {
    Page<Review> reviews(long whiskeyId, Pageable pageable);
}