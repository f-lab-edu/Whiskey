package com.whiskey.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.whiskey.payment.client.PaymentClient;
import com.whiskey.payment.dto.PaymentConfirmRequest;
import com.whiskey.payment.dto.PaymentResponse;
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
public class PaymentRetryService {

    private final PaymentClient paymentClient;

    @Retryable(
        retryFor = {RetryablePaymentException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000)
    )
    public PaymentResponse confirmPayment(String paymentKey, String orderId, Long amount)
        throws JsonProcessingException {
        PaymentConfirmRequest request = new PaymentConfirmRequest(
            paymentKey,
            orderId,
            amount
        );

        return paymentClient.confirmPayment(request);
    }

    @Recover
    public void recover(RetryablePaymentException e, String paymentKey, String orderId, Long amount) {

    }
}
