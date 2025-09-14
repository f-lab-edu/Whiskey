package com.whiskey.config;

import com.whiskey.batch.ReviewProcessor;
import com.whiskey.batch.ReviewReader;
import com.whiskey.batch.ReviewWriter;
import com.whiskey.batch.dto.ReviewBatchRequest;
import com.whiskey.domain.review.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final ReviewReader reviewReader;
    private final ReviewProcessor reviewProcessor;
    private final ReviewWriter reviewWriter;

    @Bean
    public Job dummyDataJob() {
       return new JobBuilder("dummyDataJob", jobRepository).start(reviewStep()).build();
    }

    @Bean
    public Step reviewStep() {
        return new StepBuilder("reviewStep", jobRepository)
            .<ReviewBatchRequest, Review>chunk(10, transactionManager)
            .reader(reviewReader)
            .processor(reviewProcessor)
            .writer(reviewWriter).build();
    }
}