package com.whiskey.domain.payment.service;

import com.whiskey.domain.order.Order;
import com.whiskey.domain.payment.Payment;
import com.whiskey.payment.client.PaymentClient;
import com.whiskey.payment.dto.PaymentCancelRequest;
import com.whiskey.payment.dto.PaymentCancelResponse;
import com.whiskey.payment.dto.PaymentResponse;
import com.whiskey.payment.dto.PaymentStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCompensationService {

    private final PaymentClient paymentClient;

    // completePayment 트랜잭션과 별도로 동작해야 함
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelPayment(Payment payment, Order order, PaymentResponse response) {
        PaymentStatusResponse statusResponse = paymentClient.checkPaymentStatus(response.paymentKey());

        if(!statusResponse.isApproved()) {
            log.info("결제가 승인 상태가 아님");
            return;
        }

        PaymentCancelRequest cancelRequest = new PaymentCancelRequest(
            response.paymentKey(),
            response.orderId(),
            "DB 저장 실패로 인한 결제 자동 취소 처리"
        );

        PaymentCancelResponse cancelResponse = paymentClient.cancelPayment(cancelRequest);

        if("CANCELED".equals(cancelResponse.status())) {
            payment.cancelPayment();
            order.cancelReservation();
            log.info("보상 트랜잭션 성공 - 결제 취소 완료");
        }
    }
}