package com.whiskey.payment.controller;

import com.whiskey.annotation.CurrentMemberId;
import com.whiskey.domain.member.repository.MemberRepository;
import com.whiskey.domain.payment.service.PaymentService;
import com.whiskey.payment.dto.PaymentPrepareRequest;
import com.whiskey.payment.dto.PaymentPrepareResponse;
import com.whiskey.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final MemberRepository memberRepository;

    @PostMapping("/prepare")
    public ApiResponse<PaymentPrepareResponse> prepare(@Valid @RequestBody PaymentPrepareRequest request, @CurrentMemberId
        Long memberId) {

        return null;
    }
}
