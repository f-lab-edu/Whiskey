package com.whiskey.whiskey.controller;

import com.whiskey.domain.whiskey.dto.CaskRegisterCommand;
import com.whiskey.domain.whiskey.dto.WhiskeyInfo;
import com.whiskey.domain.whiskey.dto.WhiskeyRegisterCommand;
import com.whiskey.domain.whiskey.dto.WhiskeySearchCommand;
import com.whiskey.whiskey.dto.WhiskeyRegisterDto;
import com.whiskey.whiskey.dto.WhiskeyResponseDto;
import com.whiskey.whiskey.dto.WhiskeySearchDto;
import com.whiskey.domain.whiskey.service.WhiskeyService;
import com.whiskey.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class WhiskeyController {

    private final WhiskeyService whiskeyService;

//    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/whiskey")
    @Operation(summary = "위스키 등록", description = "위스키를 등록합니다.")
    public ApiResponse<Void> register(@Valid @RequestBody WhiskeyRegisterDto whiskeyDto) {
        WhiskeyRegisterCommand command = new WhiskeyRegisterCommand(
            whiskeyDto.distillery(),
            whiskeyDto.name(),
            whiskeyDto.country(),
            whiskeyDto.age(),
            whiskeyDto.maltType(),
            whiskeyDto.abv(),
            whiskeyDto.volume(),
            whiskeyDto.description(),
            whiskeyDto.casks().stream()
                .map(caskDto -> new CaskRegisterCommand(caskDto.type()))
                .toList()
        );

        whiskeyService.register(command);
        return ApiResponse.success("위스키 등록이 완료되었습니다.");
    }

    @PutMapping("/whiskey/{id}")
    @Operation(summary = "위스키 수정", description = "위스키 ID로 위스키 정보를 수정합니다.")
    public ApiResponse<Void> update(@Parameter(description = "위스키 ID") @PathVariable("id") Long id, @Valid @RequestBody WhiskeyRegisterDto whiskeyDto) {
        WhiskeyRegisterCommand command = new WhiskeyRegisterCommand(
            whiskeyDto.distillery(),
            whiskeyDto.name(),
            whiskeyDto.country(),
            whiskeyDto.age(),
            whiskeyDto.maltType(),
            whiskeyDto.abv(),
            whiskeyDto.volume(),
            whiskeyDto.description(),
            whiskeyDto.casks().stream()
                .map(caskDto -> new CaskRegisterCommand(caskDto.type()))
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
    @Operation(summary = "위스키 조회", description = "위스키 ID로 위스키 정보를 조회합니다.")
    public ApiResponse<WhiskeyResponseDto> get(@Parameter(description = "위스키 ID") @PathVariable("id") Long id) {
        WhiskeyInfo whiskeyInfo = whiskeyService.findById(id);
        WhiskeyResponseDto response = WhiskeyResponseDto.from(whiskeyInfo);
        return ApiResponse.success("위스키를 조회하였습니다.", response);
    }

    @GetMapping("/whiskey")
    @Operation(summary = "위스키 목록 조회", description = "위스키 목록을 조회할 수 있습니다. 또, 증류소, 이름, 생산국가, 연도, 몰트 타입, 도수, 용량 등의 정보로 검색도 가능합니다.")
    public ApiResponse<List<WhiskeyResponseDto>> list(@Valid WhiskeySearchDto whiskeyDto) {
        WhiskeySearchCommand command = new WhiskeySearchCommand(
            whiskeyDto.distillery(),
            whiskeyDto.name(),
            whiskeyDto.country(),
            whiskeyDto.age(),
            whiskeyDto.maltType(),
            whiskeyDto.abv(),
            whiskeyDto.volume(),
            whiskeyDto.description()
        );

        List<WhiskeyInfo> whiskeys = whiskeyService.searchWhiskeys(command);
        List<WhiskeyResponseDto> responses = WhiskeyResponseDto.from(whiskeys);
        return ApiResponse.success("위스키 목록을 조회하였습니다.", responses);
    }
}
