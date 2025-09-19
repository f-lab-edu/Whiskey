package com.whiskey.domain.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String RATING_SUM_KEY = "whiskey:%d:rating_sum";
    private static final String REVIEW_COUNT_KEY = "whiskey:%d:review_count";
    private static final String USER_REVIEW_KEY = "whiskey:%d:user:%d";

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public void addReview(long whiskeyId, long memberId, int rating) {
        String ratingSumKey = String.format(RATING_SUM_KEY, whiskeyId);
        String userReviewKey = String.format(USER_REVIEW_KEY, whiskeyId, memberId);
        String reviewCountKey = String.format(REVIEW_COUNT_KEY, whiskeyId);

        stringRedisTemplate.opsForValue().increment(ratingSumKey, rating);
        stringRedisTemplate.opsForValue().increment(reviewCountKey, 1);
        stringRedisTemplate.opsForValue().set(userReviewKey, String.valueOf(rating));
    }

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public void updateReview(long whiskeyId, long memberId, int oldRating, int newRating) {
        String ratingSumKey = String.format(RATING_SUM_KEY, whiskeyId);
        String userReviewKey = String.format(USER_REVIEW_KEY, whiskeyId, memberId);

        int difference = newRating - oldRating;
        stringRedisTemplate.opsForValue().increment(ratingSumKey, difference);

        stringRedisTemplate.opsForValue().set(userReviewKey, String.valueOf(newRating));
    }

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public void deleteReview(long whiskeyId, long memberId) {
        String ratingSumKey = String.format(RATING_SUM_KEY, whiskeyId);
        String userReviewKey = String.format(USER_REVIEW_KEY, whiskeyId, memberId);
        String reviewCountKey = String.format(REVIEW_COUNT_KEY, whiskeyId);

        String oldRating = stringRedisTemplate.opsForValue().get(userReviewKey);

        if(oldRating != null) {
            stringRedisTemplate.opsForValue().decrement(ratingSumKey, Long.parseLong(oldRating));
            stringRedisTemplate.opsForValue().decrement(reviewCountKey, 1);
            stringRedisTemplate.delete(userReviewKey);
        }
    }
}