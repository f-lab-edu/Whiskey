package com.whiskey.domain.payment.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiskey.domain.payment.dto.CompensatePaymentInfo;
import com.whiskey.domain.payment.dto.PaymentCompensationRequest;
import com.whiskey.domain.payment.service.PaymentCompensationService;
import com.whiskey.domain.payment.service.PaymentService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetryCheckScheduler {

    private final StringRedisTemplate stringRedisTemplate;
    private final PaymentCompensationService paymentCompensationService;

    private static final String COMPENSATE_KEY = "payment:compensate";

    @Scheduled(fixedDelay = 60000)
    public void retryCheck() throws JsonProcessingException {
        Set<String> returnData = getPaymentIds();

        if(returnData.isEmpty()) {
            return;
        }

        for(String jsonData : returnData) {
            CompensatePaymentInfo compensatePaymentInfo = new ObjectMapper().readValue(jsonData, CompensatePaymentInfo.class);

            try {
                PaymentCompensationRequest request = new PaymentCompensationRequest(
                    compensatePaymentInfo.paymentId(),
                    compensatePaymentInfo.orderId(),
                    compensatePaymentInfo.paymentOrderId(),
                    compensatePaymentInfo.paymentKey()
                );
                paymentCompensationService.cancelPayment(request);
            }
            catch(Exception e) {
                compensatePaymentInfo = compensatePaymentInfo.incrRetryCount();

                if(compensatePaymentInfo.retryCount() >= 5) {
                    log.error("결제 보상 트랜잭션 재시도 횟수가 5회를 초과하였습니다.");
                    // DB 테이블에 저장하여 관리자가 직접 처리
                }
                else {
                    long nowTime = System.currentTimeMillis();
                    long nextRetryAt = nowTime + ((long) compensatePaymentInfo.retryCount() * 5 * 60 * 1000);

                    stringRedisTemplate.opsForZSet().remove(COMPENSATE_KEY, jsonData);
                    stringRedisTemplate.opsForZSet().add(COMPENSATE_KEY, jsonData, nextRetryAt);

                    log.warn("결제 보상 트랜잭션 실패 - 다음 재시도 : {}", LocalDateTime.ofInstant(Instant.ofEpochMilli(nextRetryAt), ZoneId.systemDefault()));
                }
            }
        }
    }

    private Set<String> getPaymentIds() {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        long nowTime = System.currentTimeMillis();

        return zSetOperations.rangeByScore(COMPENSATE_KEY, 0, nowTime);
    }
}