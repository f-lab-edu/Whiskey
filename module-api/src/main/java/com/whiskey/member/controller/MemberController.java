package com.whiskey.member.controller;

import com.whiskey.member.dto.MemberRegisterValue;
import com.whiskey.member.service.MemberService;
import com.whiskey.response.ApiResponse;
import com.whiskey.response.enums.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "회원가입", description = "이메일과 비밀번호로 회원가입을 진행합니다.")
    public ApiResponse<Map<String, Object>> signup(@Valid @RequestBody MemberRegisterValue memberDto) {
        memberService.signup(memberDto);

        Map<String, Object> inputData = Map.of("memberName", memberDto.memberName(), "email", memberDto.email());
        return ApiResponse.success(SuccessCode.MEMBER_REGISTERED, inputData);
    }
}
