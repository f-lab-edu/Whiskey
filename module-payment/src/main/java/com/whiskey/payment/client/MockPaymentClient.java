package com.whiskey.payment.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.whiskey.payment.dto.PaymentCancelRequest;
import com.whiskey.payment.dto.PaymentCancelResponse;
import com.whiskey.payment.dto.PaymentConfirmRequest;
import com.whiskey.payment.dto.PaymentResponse;
import com.whiskey.payment.dto.PaymentStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"test", "loadtest"})
public class MockPaymentClient implements PaymentClient {

    @Override
    public PaymentResponse confirmPayment(PaymentConfirmRequest request) throws JsonProcessingException {
        log.info("[Mock] 결제 승인 - orderId: {}, amount: {}", request.orderId(), request.amount());
        return new PaymentResponse(
            "mock_payment_key_" + request.orderId(),
            request.orderId(),
            request.amount()
        );
    }

    @Override
    public PaymentStatusResponse checkPaymentStatus(String paymentKey) {
        log.info("[Mock] 결제 조회 - paymentKey: {}", paymentKey);
        return new PaymentStatusResponse("DONE");
    }

    @Override
    public PaymentCancelResponse cancelPayment(PaymentCancelRequest request) {
        log.info("[Mock] 결제 취소 - paymentKey: {}", request.paymentKey());
        return new PaymentCancelResponse("CANCELED");
    }
}