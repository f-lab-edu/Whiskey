package com.whiskey.payment.controller;

import com.whiskey.annotation.CurrentMemberId;
import com.whiskey.domain.member.repository.MemberRepository;
import com.whiskey.domain.payment.dto.PaymentConfirmCommand;
import com.whiskey.domain.payment.dto.PaymentPrepareCommand;
import com.whiskey.domain.payment.dto.PaymentPrepareResult;
import com.whiskey.domain.payment.facade.PaymentFacade;
import com.whiskey.domain.payment.service.PaymentCommandService;
import com.whiskey.payment.dto.PaymentConfirmRequest;
import com.whiskey.payment.dto.PaymentConfirmResponse;
import com.whiskey.payment.dto.PaymentPrepareRequest;
import com.whiskey.payment.dto.PaymentPrepareResponse;
import com.whiskey.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
    private final PaymentFacade paymentFacade;

    @PostMapping("/prepare")
    @Operation(summary = "결제 요청(orderId 발급)", description = "결제 요청에 사용할 orderId를 발급합니다.")
    public ApiResponse<PaymentPrepareResponse> prepare(@Valid @RequestBody PaymentPrepareRequest request, @CurrentMemberId Long memberId) {
        PaymentPrepareCommand command = new PaymentPrepareCommand(
            memberId,
            request.orderId(),
            BigDecimal.valueOf(request.amount()),
            request.description()
        );

        PaymentPrepareResult result = paymentCommandService.createPayment(command);
        PaymentPrepareResponse response = PaymentPrepareResponse.from(result);
        return ApiResponse.success("orderId 발급에 성공했습니다.", response);
    }

    @PostMapping("/confirm")
    @Operation(summary = "결제 승인", description = "토스페이먼트 API에 결제 승인을 요청합니다.")
    public ApiResponse<PaymentConfirmResponse> confirm(@Valid @RequestBody PaymentConfirmRequest request, @CurrentMemberId Long memberId) {
        PaymentConfirmCommand command = new PaymentConfirmCommand(
            memberId,
            request.orderId(),
            BigDecimal.valueOf(request.amount()),
            request.paymentKey()
        );

        paymentFacade.confirmPayment(command);
        return ApiResponse.success("결제 승인에 성공했습니다.");
    }
}