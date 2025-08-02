package com.whiskey.member.controller;

import com.whiskey.member.dto.MemberRegisterValue;
import com.whiskey.member.dto.MemberResponse;
import com.whiskey.member.service.MemberService;
import com.whiskey.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/members")
    public ApiResponse<Void> signup(@Valid @RequestBody MemberRegisterValue memberDto) {
        memberService.signup(memberDto);
        return ApiResponse.success("회원가입이 완료되었습니다.");
    }

    @GetMapping("/members/{id}")
    public ApiResponse<MemberResponse> getMemberById(@PathVariable("id") Long id) {
        MemberResponse member = memberService.getMemberById(id);
        return ApiResponse.success("회원정보를 조회하였습니다.", member);
    }
}
