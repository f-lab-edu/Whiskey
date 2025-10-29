package com.whiskey.whiskey.controller;

import com.whiskey.annotation.ActivityLog;
import com.whiskey.annotation.TargetId;
import com.whiskey.domain.log.enums.ActivityType;
import com.whiskey.domain.log.enums.TargetType;
import com.whiskey.domain.review.dto.ReviewCursorRequest;
import com.whiskey.domain.review.dto.ReviewInfo;
import com.whiskey.domain.review.enums.ReviewFilter;
import com.whiskey.domain.review.service.ReviewService;
import com.whiskey.domain.whiskey.dto.CaskCommand;
import com.whiskey.domain.whiskey.dto.WhiskeyInfo;
import com.whiskey.domain.whiskey.dto.WhiskeyCommand;
import com.whiskey.domain.whiskey.dto.WhiskeySearchCondition;
import com.whiskey.domain.review.dto.ReviewCursorResponse;
import com.whiskey.whiskey.dto.ReviewResponse;
import com.whiskey.whiskey.dto.WhiskeyRegisterRequest;
import com.whiskey.whiskey.dto.WhiskeyResponse;
import com.whiskey.whiskey.dto.WhiskeySearchRequest;
import com.whiskey.domain.whiskey.service.WhiskeyService;
import com.whiskey.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class WhiskeyController {

    private final WhiskeyService whiskeyService;
    private final ReviewService reviewService;

//    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/whiskey")
    @Operation(summary = "위스키 등록", description = "위스키를 등록합니다.")
    public ApiResponse<Void> register(@Valid @RequestBody WhiskeyRegisterRequest whiskeyDto) {
        WhiskeyCommand command = new WhiskeyCommand(
            whiskeyDto.distillery(),
            whiskeyDto.name(),
            whiskeyDto.country(),
            whiskeyDto.age(),
            whiskeyDto.maltType(),
            whiskeyDto.abv(),
            whiskeyDto.volume(),
            whiskeyDto.description(),
            whiskeyDto.casks().stream()
                .map(caskDto -> new CaskCommand(caskDto.type()))
                .toList()
        );

        whiskeyService.register(command);
        return ApiResponse.success("위스키 등록이 완료되었습니다.");
    }

    @PutMapping("/whiskey/{id}")
    @Operation(summary = "위스키 수정", description = "위스키 ID로 위스키 정보를 수정합니다.")
    public ApiResponse<Void> update(@Parameter(description = "위스키 ID") @PathVariable("id") Long id, @Valid @RequestBody WhiskeyRegisterRequest whiskeyDto) {
        WhiskeyCommand command = new WhiskeyCommand(
            whiskeyDto.distillery(),
            whiskeyDto.name(),
            whiskeyDto.country(),
            whiskeyDto.age(),
            whiskeyDto.maltType(),
            whiskeyDto.abv(),
            whiskeyDto.volume(),
            whiskeyDto.description(),
            whiskeyDto.casks().stream()
                .map(caskDto -> new CaskCommand(caskDto.type()))
                .toList()
        );

        whiskeyService.update(id, command);
        return ApiResponse.success("위스키 정보가 수정되었습니다.");
    }

    @DeleteMapping("/whiskey/{id}")
    @Operation(summary = "위스키 삭제", description = "위스키 ID로 위스키를 삭제합니다. 논리적 삭제가 아닌 물리적으로 삭제합니다.")
    public ApiResponse<Void> delete(@Parameter(description = "위스키 ID") @PathVariable("id") @NotNull Long id) {
        whiskeyService.delete(id);
        return ApiResponse.success("위스키 정보가 삭제되었습니다.");
    }

    @GetMapping("/whiskey/{id}")
    @ActivityLog(type = ActivityType.VIEW, target = TargetType.WHISKEY)
    @Operation(summary = "위스키 조회", description = "위스키 ID로 위스키 정보를 조회합니다.")
    public ApiResponse<WhiskeyResponse> get(@Parameter(description = "위스키 ID") @PathVariable("id") @TargetId Long id) {
        WhiskeyInfo whiskeyInfo = whiskeyService.findById(id);
        WhiskeyResponse response = WhiskeyResponse.from(whiskeyInfo);
        return ApiResponse.success("위스키를 조회하였습니다.", response);
    }

    @GetMapping("/whiskey")
    @Operation(summary = "위스키 목록 조회", description = "위스키 목록을 조회할 수 있습니다. 또, 증류소, 이름, 생산국가, 연도, 몰트 타입, 도수, 용량 등의 정보로 검색도 가능합니다.")
    public ApiResponse<List<WhiskeyResponse>> list(@Valid WhiskeySearchRequest whiskeyDto) {
        WhiskeySearchCondition condition = new WhiskeySearchCondition(
            whiskeyDto.distillery(),
            whiskeyDto.name(),
            whiskeyDto.country(),
            whiskeyDto.age(),
            whiskeyDto.maltType(),
            whiskeyDto.abv(),
            whiskeyDto.volume(),
            whiskeyDto.description()
        );

        List<WhiskeyInfo> whiskeys = whiskeyService.searchWhiskeys(condition);
        List<WhiskeyResponse> responses = WhiskeyResponse.from(whiskeys);
        return ApiResponse.success("위스키 목록을 조회하였습니다.", responses);
    }

    @GetMapping("/whiskey/{id}/reviews")
    public ApiResponse<ReviewCursorResponse<ReviewResponse>> reviews(
        @PathVariable("id") Long id,
        @RequestParam(name = "cursor", required = false) String cursor,
        @RequestParam(name = "size", defaultValue = "7") int size,
        @RequestParam(name = "filter", defaultValue = "ACTIVE") String filter) {

        ReviewCursorRequest reviewRequest = ReviewCursorRequest.of(cursor, size, ReviewFilter.valueOf(filter));
        ReviewCursorResponse<ReviewInfo> reviews = reviewService.getLatestReviews(id, reviewRequest);

        ReviewCursorResponse<ReviewResponse> responses = ReviewCursorResponse.of(
            reviews.data().stream().map(ReviewResponse::from).collect(Collectors.toList()),
            reviews.nextCursor(),
            reviews.hasNext()
        );

        return ApiResponse.success("리뷰 목록을 조회하였습니다.", responses);
    }
}
