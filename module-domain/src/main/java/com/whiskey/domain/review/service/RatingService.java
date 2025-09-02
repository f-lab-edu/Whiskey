package com.whiskey.domain.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String RATING_SUM_KEY = "whiskey:%d:rating_sum";
    private static final String REVIEW_COUNT_KEY = "whiskey:%d:review_count";
    private static final String USER_REVIEW_KEY = "whiskey:%d:user:%d";

    public void addReview(long whiskeyId, long memberId, int rating) {
        String ratingSumKey = String.format(RATING_SUM_KEY, whiskeyId);
        String userReviewKey = String.format(USER_REVIEW_KEY, whiskeyId, memberId);
        String reviewCountKey = String.format(REVIEW_COUNT_KEY, whiskeyId);

        stringRedisTemplate.opsForValue().increment(ratingSumKey, rating);
        stringRedisTemplate.opsForValue().increment(reviewCountKey, 1);
        stringRedisTemplate.opsForValue().set(userReviewKey, String.valueOf(rating));
    }

    public void updateReview(long whiskeyId, long memberId, int newRating) {
        String ratingSumKey = String.format(RATING_SUM_KEY, whiskeyId);
        String userReviewKey = String.format(USER_REVIEW_KEY, whiskeyId, memberId);

        String oldRating = stringRedisTemplate.opsForValue().get(userReviewKey);

        if(oldRating != null) {
            int difference = newRating - Integer.parseInt(oldRating);
            stringRedisTemplate.opsForValue().increment(ratingSumKey, difference);
        }

        stringRedisTemplate.opsForValue().set(userReviewKey, String.valueOf(newRating));
    }

    public void removeReview(Long whiskeyId, int rating) {
        String ratingSumKey = String.format(RATING_SUM_KEY, whiskeyId);
        String reviewCountKey = String.format(REVIEW_COUNT_KEY, whiskeyId);

        stringRedisTemplate.opsForValue().decrement(ratingSumKey, rating);
        stringRedisTemplate.opsForValue().decrement(reviewCountKey, 1);
    }

    public double calculateAverageRating(Long whiskeyId) {
        String ratingSumKey = String.format(RATING_SUM_KEY, whiskeyId);
        String reviewCountKey = String.format(REVIEW_COUNT_KEY, whiskeyId);

        String sum = stringRedisTemplate.opsForValue().get(ratingSumKey);
        String count = stringRedisTemplate.opsForValue().get(reviewCountKey);

        if(sum == null || count == null) {
            return 0.0;
        }

        long sumLong = Long.parseLong(sum);
        long countLong = Long.parseLong(count);

        return countLong > 0 ? Math.round((double) sumLong / countLong * 100.0) / 100.0 : 0.0;
    }
}