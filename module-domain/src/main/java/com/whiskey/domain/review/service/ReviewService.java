package com.whiskey.domain.review.service;

import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.repository.MemberRepository;
import com.whiskey.domain.review.Review;
import com.whiskey.domain.review.dto.ReviewCommand;
import com.whiskey.domain.review.dto.ReviewInfo;
import com.whiskey.domain.review.repository.ReviewRepository;
import com.whiskey.domain.whiskey.Whiskey;
import com.whiskey.domain.whiskey.repository.WhiskeyRepository;
import com.whiskey.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final WhiskeyRepository whiskeyRepository;
    private final MemberRepository memberRepository;

    private final RatingService ratingService;

    @Transactional
    public void register(ReviewCommand reviewDto) {
        Member member = checkExistMember(reviewDto.memberId());
        Whiskey whiskey = checkExistWhiskey(reviewDto.whiskeyId());

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
        ratingService.addReview(whiskey.getId(), member.getId(), reviewDto.starRate());
    }

    public Page<ReviewInfo> reviews(long whiskeyId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.reviews(whiskeyId, pageable);
        return reviews.map(ReviewInfo::from);
    }

    @Transactional
    public void update(long id, ReviewCommand reviewDto) {
        Member member = checkExistMember(reviewDto.memberId());
        Whiskey whiskey = checkExistWhiskey(reviewDto.whiskeyId());

        boolean check = reviewRepository.existsByWhiskeyIdAndMemberId(whiskey.getId(), reviewDto.memberId());
        if (!check) {
            throw ErrorCode.CONFLICT.exception("등록하신 리뷰가 없습니다.");
        }

        Review review = reviewRepository.findById(id).orElseThrow(() -> ErrorCode.NOT_FOUND.exception("리뷰를 찾을 수 없습니다."));

        review.setStarRate(reviewDto.starRate());
        review.setContent(reviewDto.content());

        ratingService.updateReview(id, member.getId(), reviewDto.starRate());
    }

    private Member checkExistMember(long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> ErrorCode.NOT_FOUND.exception("존재하지 않는 회원입니다."));
    }

    private Whiskey checkExistWhiskey(long whiskeyId) {
        return whiskeyRepository.findById(whiskeyId).orElseThrow(() -> ErrorCode.NOT_FOUND.exception("존재하지 않는 위스키입니다."));
    }
}