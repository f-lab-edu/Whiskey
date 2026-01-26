package com.whiskey.domain.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiskey.domain.order.Order;
import com.whiskey.domain.order.repository.OrderRepository;
import com.whiskey.domain.payment.Payment;
import com.whiskey.domain.payment.dto.CompensatePaymentInfo;
import com.whiskey.domain.payment.dto.PaymentCompensationRequest;
import com.whiskey.domain.payment.repository.PaymentRepository;
import com.whiskey.payment.client.PaymentClient;
import com.whiskey.payment.dto.PaymentCancelRequest;
import com.whiskey.payment.dto.PaymentCancelResponse;
import com.whiskey.payment.dto.PaymentStatusResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCompensationService {

    private final PaymentClient paymentClient;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    private final StringRedisTemplate stringRedisTemplate;

    private static final String COMPENSATE_KEY = "payment:compensate";

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

    public void compensateFailurePayment(CompensatePaymentInfo request, LocalDateTime retryAt)
        throws JsonProcessingException {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonData = objectMapper.writeValueAsString(request);
        double score = retryAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        zSetOperations.add(COMPENSATE_KEY, jsonData, score);
    }
}