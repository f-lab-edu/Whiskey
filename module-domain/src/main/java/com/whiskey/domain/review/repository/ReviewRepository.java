package com.whiskey.domain.review.repository;

import com.whiskey.domain.review.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewRepositoryCustom {

    boolean existsByWhiskeyIdAndMemberId(long whiskeyId, long memberId);
}