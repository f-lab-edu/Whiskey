package com.whiskey.domain.payment.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiskey.domain.payment.dto.CompensatePaymentInfo;
import com.whiskey.domain.payment.dto.PaymentCompensationRequest;
import com.whiskey.domain.payment.service.PaymentCompensationService;
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

    // 적재(PaymentCompensationService)와 같은 매퍼를 써야 LocalDateTime 이 역직렬화된다.
    private final ObjectMapper objectMapper;

    private static final String COMPENSATE_KEY = "payment:compensate";

    // 재시도를 소진한 항목을 옮겨두는 키. 재시도 큐에서는 빼되 기록은 남겨 관리자가 처리한다.
    private static final String DEAD_LETTER_KEY = "payment:compensate:dead";

    private static final int MAX_RETRY_COUNT = 5;
    private static final long RETRY_INTERVAL_MILLIS = 5 * 60 * 1000L;

    @Scheduled(fixedDelay = 60000)
    public void retryCheck() {
        Set<String> returnData = getPaymentIds();

        if(returnData == null || returnData.isEmpty()) {
            return;
        }

        // 한 건이 터져도 남은 건은 처리해야 하므로 항목 단위로 격리한다.
        // (스케줄 메서드가 예외를 올리면 그 틱의 나머지 항목이 통째로 밀린다)
        for(String jsonData : returnData) {
            try {
                retryOne(jsonData);
            }
            catch(Exception e) {
                log.error("결제 보상 재시도 처리 실패 - data: {}", jsonData, e);
            }
        }
    }

    private void retryOne(String jsonData) throws JsonProcessingException {
        CompensatePaymentInfo compensatePaymentInfo = objectMapper.readValue(jsonData, CompensatePaymentInfo.class);

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
            reschedule(jsonData, compensatePaymentInfo, e);
            return;
        }

        // 성공한 항목은 큐에서 지운다. 남겨두면 매 틱마다 같은 보상을 다시 시도한다.
        stringRedisTemplate.opsForZSet().remove(COMPENSATE_KEY, jsonData);
        log.info("결제 보상 트랜잭션 재시도 성공 - paymentId: {}", compensatePaymentInfo.paymentId());
    }

    private void reschedule(String jsonData, CompensatePaymentInfo compensatePaymentInfo, Exception cause)
        throws JsonProcessingException {
        CompensatePaymentInfo retried = compensatePaymentInfo.incrRetryCount();

        if(retried.retryCount() >= MAX_RETRY_COUNT) {
            // 재시도 큐에 그대로 두면 무한 재시도가 되므로 별도 키로 옮긴다.
            // TODO: 전용 테이블로 이관해 관리자가 화면에서 처리할 수 있게 한다.
            // 소진된 시도 횟수까지 기록해 옮긴다. 원본을 그대로 옮기면 로그의 횟수와 레코드가 어긋난다.
            stringRedisTemplate.opsForZSet()
                .add(DEAD_LETTER_KEY, objectMapper.writeValueAsString(retried), System.currentTimeMillis());
            stringRedisTemplate.opsForZSet().remove(COMPENSATE_KEY, jsonData);

            log.error("결제 보상 트랜잭션 재시도 {}회 소진 - 수동 처리 필요. info: {}", MAX_RETRY_COUNT, retried, cause);
            return;
        }

        long nextRetryAt = System.currentTimeMillis() + (retried.retryCount() * RETRY_INTERVAL_MILLIS);

        // 증가시킨 retryCount 를 다시 직렬화해 저장해야 재시도 횟수가 누적된다.
        // 원본 문자열을 그대로 재적재하면 retryCount 가 0에 머물러 MAX_RETRY_COUNT 가드가 영원히 걸리지 않는다.
        // 유실보다 중복이 안전하므로 새 항목을 먼저 넣고 기존 항목을 지운다.
        stringRedisTemplate.opsForZSet().add(COMPENSATE_KEY, objectMapper.writeValueAsString(retried), nextRetryAt);
        stringRedisTemplate.opsForZSet().remove(COMPENSATE_KEY, jsonData);

        log.warn("결제 보상 트랜잭션 실패({}/{}) - 다음 재시도 : {}",
            retried.retryCount(), MAX_RETRY_COUNT,
            LocalDateTime.ofInstant(Instant.ofEpochMilli(nextRetryAt), ZoneId.systemDefault()), cause);
    }

    private Set<String> getPaymentIds() {
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        long nowTime = System.currentTimeMillis();

        return zSetOperations.rangeByScore(COMPENSATE_KEY, 0, nowTime);
    }
}
