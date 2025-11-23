package com.whiskey.domain.payment.service;

import com.whiskey.domain.order.Order;
import com.whiskey.domain.order.repository.OrderRepository;
import com.whiskey.domain.payment.Payment;
import com.whiskey.domain.payment.dto.PaymentCompensationRequest;
import com.whiskey.domain.payment.repository.PaymentRepository;
import com.whiskey.payment.client.PaymentClient;
import com.whiskey.payment.dto.PaymentCancelRequest;
import com.whiskey.payment.dto.PaymentCancelResponse;
import com.whiskey.payment.dto.PaymentStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCompensationService {

    private final PaymentClient paymentClient;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void cancelPayment(PaymentCompensationRequest request) {
        PaymentStatusResponse statusResponse = paymentClient.checkPaymentStatus(request.paymentKey());

        if(!statusResponse.isApproved()) {
            log.info("결제가 승인 상태가 아님 - paymentKey: {}", request.paymentKey());
            return;
        }

        PaymentCancelRequest cancelRequest = new PaymentCancelRequest(
            request.paymentKey(),
            request.paymentOrderId(),
            "DB 저장 실패로 인한 결제 자동 취소 처리"
        );

        PaymentCancelResponse cancelResponse = paymentClient.cancelPayment(cancelRequest);

        if(cancelResponse.isCanceled()) {
            Payment payment = paymentRepository.findById(request.paymentId()).orElseThrow();
            Order order = orderRepository.findById(request.orderId()).orElseThrow();

            payment.cancelPayment();
            order.cancelReservation();
            log.info("보상 트랜잭션 성공 - 결제 취소 완료: paymentId={}", request.paymentId());
        }
    }
}