package com.whiskey.domain.review.repository;

import com.whiskey.domain.review.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByWhiskeyIdAndMemberId(long whiskeyId, long memberId);

    List<Review> findByWhiskeyId(long whiskeyId);
}