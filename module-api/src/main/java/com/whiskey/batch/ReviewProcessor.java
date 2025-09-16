package com.whiskey.batch;

import com.whiskey.batch.dto.ReviewBatchRequest;
import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.repository.MemberRepository;
import com.whiskey.domain.review.Review;
import com.whiskey.domain.review.repository.ReviewRepository;
import com.whiskey.domain.whiskey.Whiskey;
import com.whiskey.domain.whiskey.repository.WhiskeyRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class ReviewProcessor implements ItemProcessor<ReviewBatchRequest, Review> {

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final WhiskeyRepository whiskeyRepository;

    private final List<String> reviewTemplate = Arrays.asList(
        "정말 훌륭한 위스키네요! 깊은 맛이 인상적입니다.",
        "부드러운 목넘김과 균형잡힌 맛이 좋아요.",
        "스모키한 향이 독특하고 매력적이에요.",
        "가격 대비 만족스러운 위스키입니다.",
        "초보자도 마시기 좋은 부드러운 맛이에요.",
        "복합적인 풍미가 정말 인상깊었습니다.",
        "달콤한 과일향이 매력적인 위스키네요.",
        "오크통 숙성의 깊이를 느낄 수 있어요.",
        "특별한 날에 마시기 좋은 프리미엄 위스키입니다.",
        "개성있는 맛으로 기억에 남을 것 같아요.",
        "진한 색깔만큼 깊은 맛을 자랑하네요.",
        "향부터 마무리까지 완벽한 밸런스입니다.",
        "강렬한 첫인상과 부드러운 마무리가 좋네요.",
        "위스키 애호가라면 꼭 한번 마셔보세요.",
        "기대했던 것보다 더 좋은 위스키였어요!",
        "친구들과 함께 마시기 좋은 위스키입니다.",
        "숙성도가 잘 느껴지는 깊은 맛이에요.",
        "선물용으로도 손색없는 훌륭한 위스키네요."
    );

    private final Random random = new Random();

    @Override
    public Review process(ReviewBatchRequest reviewDto) throws Exception {
        boolean exists = reviewRepository.existsByWhiskeyIdAndMemberId(
            reviewDto.whiskeyId(),
            reviewDto.memberId()
        );

        if(exists) {
            log.debug("이미 존재하는 리뷰입니다. whiskeyId: {}, memberId: {}", reviewDto.whiskeyId(), reviewDto.memberId());
            return null;
        }

        Member member = memberRepository.findById(reviewDto.memberId()).get();
        Whiskey whiskey = whiskeyRepository.findById(reviewDto.whiskeyId()).get();

        int starRate = random.nextInt(5) + 1;
        String content = reviewTemplate.get(random.nextInt(reviewTemplate.size()));

        Review review = Review.builder()
            .whiskey(whiskey)
            .member(member)
            .starRate(starRate)
            .content(content)
            .build();

        log.debug("리뷰 생성: whiskeyId={}, memberId={}, starRate={}", reviewDto.whiskeyId(), reviewDto.memberId(), starRate);
        return review;
    }
}