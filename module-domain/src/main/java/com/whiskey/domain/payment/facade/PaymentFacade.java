package com.whiskey.domain.payment.facade;

import com.whiskey.domain.order.exception.OrderProcessingException;
import com.whiskey.domain.order.service.OrderService;
import com.whiskey.domain.payment.Payment;
import com.whiskey.domain.payment.dto.PaymentCompensationRequest;
import com.whiskey.domain.payment.dto.PaymentCompleteRequest;
import com.whiskey.domain.payment.dto.PaymentConfirmCommand;
import com.whiskey.domain.payment.exception.PaymentProcessingException;
import com.whiskey.domain.payment.service.PaymentCompensationService;
import com.whiskey.domain.payment.service.PaymentService;
import com.whiskey.payment.client.PaymentClient;
import com.whiskey.payment.dto.PaymentConfirmRequest;
import com.whiskey.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentFacade {

    private final PaymentClient paymentClient;
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PaymentCompensationService paymentCompensationService;

    // 결제 승인 요청
    public void confirmPayment(PaymentConfirmCommand command) {
        // 1. 결제와 유효성 검사
        Payment payment = paymentService.validatePayment(command);

        // 2. PG 결제 승인 요청(트랜잭션 밖에서)
        PaymentResponse paymentResponse = requestPaymentConfirm(command);

        // 3. PG 결제 성공하면 DB 업데이트(트랜잭션 시작)
        PaymentCompleteRequest request = PaymentCompleteRequest.from(payment, paymentResponse);

        // 4. 결제 완료 처리
        completePaymentProcess(request);

        // 5. 주문 확정
        confirmOrderProcess(request);
    }

    private PaymentResponse requestPaymentConfirm(PaymentConfirmCommand command) {
        try {
            PaymentConfirmRequest request = new PaymentConfirmRequest(
                command.paymentKey(),
                command.orderId(),
                command.amount().longValueExact()
            );

            return paymentClient.confirmPayment(request);

        } catch (Exception e) {
            log.error("PG 결제 승인 실패 - orderId: {}", command.orderId(), e);
            throw new RuntimeException("결제 승인에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private void completePaymentProcess(PaymentCompleteRequest request) {
        try {
            paymentService.completePayment(request);
        }
        catch (Exception e) {
            compensatePayment(request, "결제 완료 처리 실패");
            throw new PaymentProcessingException("결제 처리 중 오류가 발생했습니다.", e);
        }
    }

    private void confirmOrderProcess(PaymentCompleteRequest request) {
        try {
            orderService.confirmReservation(request.orderId());
        }
        catch (Exception e) {
            compensatePayment(request, "주문 확정 처리 실패");
            throw new OrderProcessingException("주문 처리 중 오류가 발생했습니다.", e);
        }
    }

    private void compensatePayment(PaymentCompleteRequest request, String reason) {
        log.warn("보상 트랜잭션 시작 - reason : {}, paymentOrderId: {}", reason, request.paymentOrderId());

        PaymentCompensationRequest compensationRequest = request.toCompensationRequest();

        try {
            paymentCompensationService.cancelPayment(compensationRequest);
        }
        catch (Exception e) {
            log.error("보상 트랜잭션 실패", e);
            compensationFailureProcess(request, reason, e);
        }
    }

    private void compensationFailureProcess(PaymentCompleteRequest request, String reason, Exception e) {
        log.error("보상 트랜잭션 실패 후처리 - DLQ에 저장");
        
        // Redis sorted set으로 처리
    }
}
