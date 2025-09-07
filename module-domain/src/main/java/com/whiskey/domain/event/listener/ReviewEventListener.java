package com.whiskey.domain.event.listener;

import com.whiskey.domain.event.ReviewRegisteredEvent;
import com.whiskey.domain.review.service.RatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventListener {

    private final RatingService ratingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReviewRegistered(ReviewRegisteredEvent event) {
        ratingService.addReview(event.whiskeyId(), event.memberId(), event.starRate());
    }
}