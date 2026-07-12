package com.whiskey.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.service.MemberService;
import com.whiskey.domain.review.Review;
import com.whiskey.domain.review.dto.ReviewCommand;
import com.whiskey.domain.review.repository.ReviewRepository;
import com.whiskey.domain.whiskey.Whiskey;
import com.whiskey.domain.whiskey.service.WhiskeyService;
import com.whiskey.exception.BusinessException;
import com.whiskey.exception.CommonErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

class ReviewServiceTest {

    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final WhiskeyService whiskeyService = mock(WhiskeyService.class);
    private final MemberService memberService = mock(MemberService.class);
    private final ReviewService reviewService = new ReviewService(
        reviewRepository, whiskeyService, memberService, mock(ApplicationEventPublisher.class));

    private Review reviewOwnedBy(long ownerId) {
        Member owner = mock(Member.class);
        when(owner.getId()).thenReturn(ownerId);
        Review review = mock(Review.class);
        when(review.getMember()).thenReturn(owner);
        return review;
    }

    @Test
    @DisplayName("본인 리뷰가 아니면 수정 시 인가 실패(FORBIDDEN, 403)")
    void update_다른회원_FORBIDDEN() {
        Review review = reviewOwnedBy(1L);
        when(memberService.getMember(anyLong())).thenReturn(mock(Member.class));
        when(whiskeyService.checkExistWhiskey(anyLong())).thenReturn(mock(Whiskey.class));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        // 리뷰 소유자(1L)와 다른 회원(2L)이 수정 요청
        ReviewCommand command = new ReviewCommand(10L, 2L, 5, "content");

        assertThatThrownBy(() -> reviewService.update(1L, command))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertForbidden((BusinessException) ex));
    }

    @Test
    @DisplayName("본인 리뷰가 아니면 삭제 시 인가 실패(FORBIDDEN, 403)")
    void delete_다른회원_FORBIDDEN() {
        Review review = reviewOwnedBy(1L);
        Whiskey whiskey = mock(Whiskey.class);
        when(whiskey.getId()).thenReturn(10L);
        when(review.getWhiskey()).thenReturn(whiskey);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(whiskeyService.checkExistWhiskey(anyLong())).thenReturn(mock(Whiskey.class));

        // 리뷰 소유자(1L)와 다른 회원(2L)이 삭제 요청
        assertThatThrownBy(() -> reviewService.delete(1L, 2L))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertForbidden((BusinessException) ex));
    }

    private void assertForbidden(BusinessException be) {
        assertThat(be.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN);
        assertThat(be.getErrorCode().getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
