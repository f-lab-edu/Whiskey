package com.whiskey.review.controller;

import com.whiskey.domain.review.dto.ReviewCommand;
import com.whiskey.domain.review.service.ReviewService;
import com.whiskey.response.ApiResponse;
import com.whiskey.review.dto.ReviewRegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
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
    public ApiResponse<Void> register(@Valid @RequestBody ReviewRegisterRequest reviewDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long memberId = Long.parseLong(authentication.getName());

        ReviewCommand command = new ReviewCommand(
            reviewDto.whiskeyId(),
            memberId,
            reviewDto.starRate(),
            reviewDto.content()
        );

        reviewService.register(command);
        return ApiResponse.success("리뷰 등록이 완료되었습니다.");
    }
}