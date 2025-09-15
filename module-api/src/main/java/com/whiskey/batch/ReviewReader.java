package com.whiskey.batch;

import com.whiskey.batch.dto.ReviewBatchRequest;
import com.whiskey.domain.member.repository.MemberRepository;
import com.whiskey.domain.whiskey.Whiskey;
import com.whiskey.domain.whiskey.repository.WhiskeyRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Slf4j
public class ReviewReader implements ItemReader<ReviewBatchRequest> {

    private WhiskeyRepository whiskeyRepository;
    private MemberRepository memberRepository;

    private final Queue<ReviewBatchRequest> reviewQueue;
    private boolean initialized = false;

    private final Random random = new Random();

    public ReviewReader() {
        this.reviewQueue = new LinkedList<>();
    }

    @Override
    public ReviewBatchRequest read() {
        if(!initialized) {
            init();
            initialized = true;
        }

        return reviewQueue.poll();
    }

    private void init() {
        List<Long> whiskeyIds = getRandomWhiskeyId();

        // 3L은 개발자 계정
        List<Long> memberIds = Arrays.asList(1L, 2L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L);

        for(Long whiskeyId : whiskeyIds) {
            Collections.shuffle(memberIds);
            int reviewCount = random.nextInt(10) + 1;

            for(int i=0;i<Math.min(reviewCount, memberIds.size()); i++) {
                reviewQueue.offer(ReviewBatchRequest.of(whiskeyId, memberIds.get(i)));
            }
        }

        log.info("총{}개의 리뷰 데이터를 생성했습니다.", reviewQueue.size());
    }

    private List<Long> getRandomWhiskeyId() {
        List<Long> whiskeyIds = whiskeyRepository.findAll()
            .stream()
            .map(Whiskey::getId)
            .collect(Collectors.toList());

        if(whiskeyIds.isEmpty()) {
            throw new RuntimeException("등록된 위스키가 없습니다.");
        }

        Collections.shuffle(whiskeyIds);
        int count = Math.min(random.nextInt(2) + 2, whiskeyIds.size());
        return whiskeyIds.subList(0, count);
    }
}