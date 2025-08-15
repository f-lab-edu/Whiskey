package com.whiskey.domain.review.service;

import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.repository.MemberRepository;
import com.whiskey.domain.review.Review;
import com.whiskey.domain.review.dto.ReviewCommand;
import com.whiskey.domain.review.repository.ReviewRepository;
import com.whiskey.domain.whiskey.Whiskey;
import com.whiskey.domain.whiskey.repository.WhiskeyRepository;
import com.whiskey.exception.ErrorCode;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final WhiskeyRepository whiskeyRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void register(ReviewCommand reviewDto) {
        Member member = memberRepository.findById(reviewDto.memberId())
            .orElseThrow(() -> ErrorCode.NOT_FOUND.exception("존재하지 않는 회원입니다."));

        Whiskey whiskey = whiskeyRepository.findById(reviewDto.whiskeyId())
            .orElseThrow(() -> ErrorCode.NOT_FOUND.exception("존재하지 않는 위스키입니다."));

        boolean check = reviewRepository.existsByWhiskeyIdAndMemberId(whiskey.getId(), reviewDto.memberId());
        if (check) {
            throw ErrorCode.CONFLICT.exception("이미 리뷰를 등록하셨습니다.");
        }

        Review review = Review.builder()
            .whiskey(whiskey)
            .member(member)
            .starRate(reviewDto.starRate())
            .content(reviewDto.content())
            .build();

        reviewRepository.save(review);

        updateWhiskeyRating(whiskey);
    }

    private void updateWhiskeyRating(Whiskey whiskey) {
        List<Review> reviews = reviewRepository.findByWhiskeyId(whiskey.getId());

        if(reviews.isEmpty()) {
            whiskey.updateRating(0.0, 0);
        }
        else {
            double avgRating = reviews.stream()
                .mapToInt(Review::getStarRate)
                .average()
                .orElse(0.0);

            whiskey.updateRating(avgRating, reviews.size());
        }
    }
}