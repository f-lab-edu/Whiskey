package com.whiskey.batch.controller;

import com.whiskey.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job job;

    @PostMapping("/api/batch/review-dummy-data")
    public ApiResponse<Void> runReviewBatch() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(job, jobParameters);
            return ApiResponse.success("배치 실행 성공");
        }
        catch(Exception e) {
            log.error(e.getMessage());
            return ApiResponse.failure("500", "배치 실행 실패");
        }
    }
}