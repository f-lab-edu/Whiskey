package com.whiskey.domain.review.service;

import com.whiskey.domain.review.enums.ReviewSortType;
import com.whiskey.domain.review.event.ReviewDeletedEvent;
import com.whiskey.domain.review.event.ReviewRegisteredEvent;
import com.whiskey.domain.review.event.ReviewUpdatedEvent;
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

    public ReviewCursorResponse<ReviewInfo> searchReviews(ReviewCursorRequest request) {
        List<Review> reviews = fetchReviewsBySortType(request);
        return buildReviewCursorResponse(reviews, request);
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
        Review review = checkExistReview(id);
        Whiskey whiskey = checkExistWhiskey(review.getWhiskey().getId());

        if(!review.getMember().getId().equals(memberId)) {
            throw ErrorCode.UNAUTHORIZED.exception("본인의 리뷰만 삭제가능합니다.");
        }

        review.delete();
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

    private List<Review> fetchReviewsBySortType(ReviewCursorRequest request) {
        whiskeyRepository.findById(request.whiskeyId()).orElseThrow(() -> ErrorCode.NOT_FOUND.exception("위스키를 찾을 수 없습니다."));

        return switch(request.sortType()) {
            case LATEST -> ReviewRepository.findLatestReviews(request);
            case RATING_HIGH -> ReviewRepository.findByHighestRating(request);
            case RATING_LOW -> ReviewRepository.findByLowestRating(request);
        };
    }

    private ReviewCursorResponse<ReviewInfo> buildReviewCursorResponse(List<Review> reviews, ReviewCursorRequest request) {
        boolean hasNext = reviews.size() > request.size();
        if(hasNext) {
            reviews.remove(reviews.size() - 1);
        }

        String nextCursor = null;
        if(hasNext && !reviews.isEmpty()) {
            Review lastReview = reviews.get(reviews.size() - 1);
            nextCursor = createNextCursor(lastReview, request.sortType());
        }

        List<ReviewInfo> reviewInfos = reviews.stream().map(ReviewInfo::from).collect(Collectors.toList());
        return ReviewCursorResponse.of(reviewInfos, nextCursor, hasNext);
    }

    // 정렬타입에 맞춰 다음 커서 생성
    private String createNextCursor(Review lastReview, ReviewSortType sortType) {
        return switch(sortType) {
            case LATEST -> String.valueOf(lastReview.getId());
            case RATING_HIGH, RATING_LOW -> lastReview.getStarRate() + "_" + lastReview.getId();
        };
    }
}