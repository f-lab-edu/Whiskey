package com.whiskey.payment.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.whiskey.payment.dto.PaymentCancelRequest;
import com.whiskey.payment.dto.PaymentCancelResponse;
import com.whiskey.payment.dto.PaymentConfirmRequest;
import com.whiskey.payment.dto.PaymentResponse;
import com.whiskey.payment.dto.PaymentStatusResponse;

public interface PaymentClient {

    PaymentResponse confirmPayment(PaymentConfirmRequest request) throws JsonProcessingException;

    PaymentStatusResponse checkPaymentStatus(String paymentKey);

    PaymentCancelResponse cancelPayment(PaymentCancelRequest request);
}