package com.whiskey.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.whiskey.payment.client.TossPaymentClient;
import com.whiskey.payment.dto.TossPaymentConfirmRequest;
import com.whiskey.payment.dto.TossPaymentResponse;
import com.whiskey.payment.exception.RetryablePaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TossPaymentService {

    private final TossPaymentClient tossPaymentClient;

    @Retryable(
        retryFor = {RetryablePaymentException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000)
    )
    public TossPaymentResponse confirmPayment(String paymentKey, String orderId, Long amount)
        throws JsonProcessingException {
        TossPaymentConfirmRequest request = TossPaymentConfirmRequest.builder()
            .paymentKey(paymentKey)
            .orderId(orderId)
            .amount(amount)
            .build();

        return tossPaymentClient.confirmPayment(request);
    }

    @Recover
    public void recover(RetryablePaymentException e, String paymentKey, String orderId, Long amount) {

    }
}
