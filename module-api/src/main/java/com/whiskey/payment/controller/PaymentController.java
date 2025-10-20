package com.whiskey.payment.controller;

import com.whiskey.annotation.CurrentMemberId;
import com.whiskey.domain.member.repository.MemberRepository;
import com.whiskey.domain.payment.Payment;
import com.whiskey.domain.payment.dto.PaymentCommand;
import com.whiskey.domain.payment.service.PaymentCommandService;
import com.whiskey.payment.dto.PaymentPrepareRequest;
import com.whiskey.payment.dto.PaymentPrepareResponse;
import com.whiskey.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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

    private final PaymentCommandService paymentCommandService;
    private final MemberRepository memberRepository;

    @PostMapping("/prepare")
    @Operation(summary = "결제 요청(orderId 발급)", description = "결제 요청에 사용할 orderId를 발급합니다.")
    public ApiResponse<PaymentPrepareResponse> prepare(@Valid @RequestBody PaymentPrepareRequest request, @CurrentMemberId Long memberId) {
        PaymentCommand command = new PaymentCommand(
            memberId,
            request.orderId(),
            request.amount(),
            request.description()
        );

        paymentCommandService.createPayment(command);
        return null;
    }
}
