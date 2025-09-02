package com.whiskey.review.controller;

import com.whiskey.annotation.CurrentMemberId;
import com.whiskey.domain.review.dto.ReviewCommand;
import com.whiskey.domain.review.service.ReviewService;
import com.whiskey.response.ApiResponse;
import com.whiskey.review.dto.ReviewRegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/review")
    @Operation(summary = "위스키 리뷰 등록", description = "위스키 리뷰를 등록합니다.")
    public ApiResponse<Void> register(@Valid @RequestBody ReviewRegisterRequest reviewDto, @CurrentMemberId Long memberId) {
        ReviewCommand command = new ReviewCommand(
            reviewDto.whiskeyId(),
            memberId,
            reviewDto.starRate(),
            reviewDto.content()
        );

        reviewService.register(command);
        return ApiResponse.success("리뷰 등록이 완료되었습니다.");
    }

    @PutMapping("/review/{id}")
    @Operation(summary = "위스키 리뷰 수정", description = "위스키 리뷰를 수정합니다.")
    public ApiResponse<Void> update(@Parameter(description = "리뷰 ID") @PathVariable("id") Long id, @Valid @RequestBody ReviewRegisterRequest reviewDto, @CurrentMemberId Long memberId) {
        ReviewCommand command = new ReviewCommand(
            reviewDto.whiskeyId(),
            memberId,
            reviewDto.starRate(),
            reviewDto.content()
        );

        reviewService.update(id, command);
        return ApiResponse.success("리뷰 수정이 완료되었습니다.");
    }
}