package com.whiskey.domain.review.service;

import com.whiskey.domain.event.ReviewDeletedEvent;
import com.whiskey.domain.event.ReviewRegisteredEvent;
import com.whiskey.domain.event.ReviewUpdatedEvent;
import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.repository.MemberRepository;
import com.whiskey.domain.review.Review;
import com.whiskey.domain.review.dto.ReviewCommand;
import com.whiskey.domain.review.dto.ReviewCursorRequest;
import com.whiskey.domain.review.dto.ReviewCursorResponse;
import com.whiskey.domain.review.dto.ReviewInfo;
import com.whiskey.domain.review.repository.ReviewRepository;
import com.whiskey.domain.whiskey.Whiskey;
import com.whiskey.domain.whiskey.repository.WhiskeyRepository;
import com.whiskey.exception.ErrorCode;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final WhiskeyRepository whiskeyRepository;
    private final MemberRepository memberRepository;

    private final ApplicationEventPublisher eventPublisher;

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
        eventPublisher.publishEvent(new ReviewRegisteredEvent(whiskey.getId(), member.getId(), reviewDto.starRate()));
    }

    public ReviewCursorResponse<ReviewInfo> getLatestReviews(long whiskeyId, ReviewCursorRequest reviewRequest) {
        Long cursorId = null;

        if(reviewRequest.cursor() != null) {
            cursorId = Long.parseLong(reviewRequest.cursor());
        }

        List<Review> reviews = reviewRepository.findLatestReviews(whiskeyId, cursorId, reviewRequest.size());

        boolean hasNext = reviews.size() > reviewRequest.size();
        if(hasNext) {
            reviews.remove(reviews.size() - 1);
        }

        String nextCursor = null;
        if(hasNext && !reviews.isEmpty()) {
            Review lastReview = reviews.get(reviews.size() - 1);
            nextCursor = String.valueOf(lastReview.getId());
        }

        List<ReviewInfo> reviewInfos = reviews.stream().map(ReviewInfo::from).collect(Collectors.toList());
        return ReviewCursorResponse.of(reviewInfos, nextCursor, hasNext);
    }

    @Transactional
    public void update(long id, ReviewCommand reviewDto) {
        Member member = checkExistMember(reviewDto.memberId());
        Whiskey whiskey = checkExistWhiskey(reviewDto.whiskeyId());
        Review review = checkExistReview(id);

        if(!review.getMember().getId().equals(reviewDto.memberId())) {
            throw ErrorCode.UNAUTHORIZED.exception("본인의 리뷰만 수정가능합니다.");
        }

        if(!whiskey.getId().equals(reviewDto.whiskeyId())) {
            throw ErrorCode.NOT_FOUND.exception("잘못된 위스키 정보입니다.");
        }

        int oldRating = review.getStarRate();

        review.setStarRate(reviewDto.starRate());
        review.setContent(reviewDto.content());

        eventPublisher.publishEvent(new ReviewUpdatedEvent(whiskey.getId(), member.getId(), oldRating, reviewDto.starRate()));
    }

    @Transactional
    public void delete(long id, long memberId) {
        Member member = checkExistMember(memberId);
        Review review = checkExistReview(id);
        Whiskey whiskey = checkExistWhiskey(review.getWhiskey().getId());

        if(!review.getMember().getId().equals(memberId)) {
            throw ErrorCode.UNAUTHORIZED.exception("본인의 리뷰만 삭제가능합니다.");
        }

        reviewRepository.deleteById(id);
        eventPublisher.publishEvent(new ReviewDeletedEvent(whiskey.getId(), memberId));
    }

    private Member checkExistMember(long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> ErrorCode.NOT_FOUND.exception("존재하지 않는 회원입니다."));
    }

    private Whiskey checkExistWhiskey(long whiskeyId) {
        return whiskeyRepository.findById(whiskeyId).orElseThrow(() -> ErrorCode.NOT_FOUND.exception("존재하지 않는 위스키입니다."));
    }

    private Review checkExistReview(long reviewId) {
        return reviewRepository.findById(reviewId).orElseThrow(() -> ErrorCode.NOT_FOUND.exception("리뷰를 찾을 수 없습니다."));
    }
}