package com.whiskey.batch;

import com.whiskey.domain.review.Review;
import com.whiskey.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class ReviewWriter implements ItemWriter<Review> {

    private final ReviewRepository reviewRepository;

    @Override
    public void write(Chunk<? extends Review> chunk) throws Exception {
        log.info("{}개의 리뷰를 저장합니다.", chunk.size());

        reviewRepository.saveAll(chunk.getItems());

        chunk.getItems().forEach(review ->
            log.debug("리뷰 저장 완료 - 위스키ID: {}, 사용자ID: {}, 평점: {}",
                review.getWhiskey().getId(),
                review.getMember().getId(),
                review.getStarRate())
        );

        log.info("{}개의 리뷰 저장이 완료되었습니다.", chunk.size());
    }
}
