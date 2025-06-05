package com.whiskey.member.controller;

import com.whiskey.member.dto.MemberRegisterValue;
import com.whiskey.member.service.MemberService;
import com.whiskey.response.ApiResponse;
import jakarta.validation.Valid;
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
    public ApiResponse<Void> signup(@Valid @RequestBody MemberRegisterValue memberDto) {
        memberService.signup(memberDto);
        return ApiResponse.success("회원가입에 성공하셨습니다.");
    }
}
