package com.whiskey.domain.payment.service;

import com.whiskey.domain.payment.dto.CompensatePaymentInfo;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 결제 보상 트랜잭션 재시도 큐.
 *
 * <p>payload 를 ZSet member 에 JSON 으로 넣으면 member 가 곧 식별자가 되어, 필드 하나를 바꿀 때마다
 * 레코드를 교체(remove + add)해야 하고 그걸 빠뜨리면 갱신이 조용히 사라진다. 또 저장 형식이
 * ObjectMapper 설정에 좌우돼 설정이 바뀌면 이미 쌓인 항목을 못 읽고, paymentId 로 단건 조회도 안 된다.
 * 그래서 payload 는 Hash 에 두고 ZSet 은 예정 시각 인덱스로만 쓴다.
 *
 * <pre>
 * payment:compensate:payload:{paymentId}  Hash  payload. retryCount 는 HINCRBY 로 원자적으로 증가시킨다.
 * payment:compensate:schedule             ZSet  member = paymentId, score = 재시도 예정 시각(epoch millis).
 * payment:compensate:dead                 ZSet  member = paymentId, score = 소진 시각. payload 는 남겨 조회할 수 있게 한다.
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompensationQueue {

    private static final String PAYLOAD_KEY_PREFIX = "payment:compensate:payload:";
    private static final String SCHEDULE_KEY = "payment:compensate:schedule";
    private static final String DEAD_LETTER_KEY = "payment:compensate:dead";

    // paymentId 는 payload 키에 들어가므로 필드로 중복 저장하지 않는다.
    private static final String ORDER_ID = "orderId";
    private static final String PAYMENT_ORDER_ID = "paymentOrderId";
    private static final String PAYMENT_KEY = "paymentKey";
    private static final String REASON = "reason";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String FAILED_AT = "failedAt";
    private static final String RETRY_COUNT = "retryCount";

    private final StringRedisTemplate stringRedisTemplate;

    public void save(CompensatePaymentInfo info, LocalDateTime retryAt) {
        hashOperations().putAll(payloadKey(info.paymentId()), toFields(info));
        stringRedisTemplate.opsForZSet().add(SCHEDULE_KEY, member(info.paymentId()), toScore(retryAt));
    }

    /** 재시도 예정 시각이 지난 항목. 스케줄에만 남고 payload 가 사라진 항목은 정리한다. */
    public List<CompensatePaymentInfo> findDue(LocalDateTime now) {
        Set<String> paymentIds = stringRedisTemplate.opsForZSet().rangeByScore(SCHEDULE_KEY, 0, toScore(now));

        if(paymentIds == null || paymentIds.isEmpty()) {
            return List.of();
        }

        List<CompensatePaymentInfo> due = new ArrayList<>();

        for(String paymentId : paymentIds) {
            Map<String, String> fields = hashOperations().entries(PAYLOAD_KEY_PREFIX + paymentId);

            if(fields.isEmpty()) {
                log.warn("보상 재시도 스케줄에 payload 가 없어 정리 - paymentId: {}", paymentId);
                stringRedisTemplate.opsForZSet().remove(SCHEDULE_KEY, paymentId);
                continue;
            }

            due.add(toInfo(Long.parseLong(paymentId), fields));
        }

        return due;
    }

    /** 재시도 횟수를 원자적으로 1 증가시키고 증가된 값을 반환한다. */
    public long incrementRetryCount(Long paymentId) {
        return hashOperations().increment(payloadKey(paymentId), RETRY_COUNT, 1);
    }

    /** ZADD 는 이미 있는 member 의 score 만 갱신하므로 remove + add 가 필요 없다. */
    public void reschedule(Long paymentId, LocalDateTime nextRetryAt) {
        stringRedisTemplate.opsForZSet().add(SCHEDULE_KEY, member(paymentId), toScore(nextRetryAt));
    }

    public void remove(Long paymentId) {
        stringRedisTemplate.opsForZSet().remove(SCHEDULE_KEY, member(paymentId));
        stringRedisTemplate.delete(payloadKey(paymentId));
    }

    /** 재시도를 소진한 항목을 재시도 대상에서 뺀다. payload 는 남겨 수동 처리에 쓸 수 있게 한다. */
    public void moveToDeadLetter(Long paymentId) {
        stringRedisTemplate.opsForZSet().add(DEAD_LETTER_KEY, member(paymentId), toScore(LocalDateTime.now()));
        stringRedisTemplate.opsForZSet().remove(SCHEDULE_KEY, member(paymentId));
    }

    private Map<String, String> toFields(CompensatePaymentInfo info) {
        Map<String, String> fields = new LinkedHashMap<>();

        fields.put(ORDER_ID, String.valueOf(info.orderId()));
        fields.put(PAYMENT_ORDER_ID, info.paymentOrderId());
        fields.put(PAYMENT_KEY, info.paymentKey());
        fields.put(REASON, info.reason());
        // 원인 예외 메시지는 없을 수 있다. Hash 값에 null 을 넣을 수 없어 빈 문자열로 둔다.
        fields.put(ERROR_MESSAGE, info.errorMessage() == null ? "" : info.errorMessage());
        // 저장 형식을 ObjectMapper 설정이 아니라 코드로 고정한다. LocalDateTime#toString 은 ISO-8601 이다.
        fields.put(FAILED_AT, info.failedAt().toString());
        fields.put(RETRY_COUNT, String.valueOf(info.retryCount()));

        return fields;
    }

    private CompensatePaymentInfo toInfo(Long paymentId, Map<String, String> fields) {
        return new CompensatePaymentInfo(
            paymentId,
            Long.parseLong(fields.get(ORDER_ID)),
            fields.get(PAYMENT_ORDER_ID),
            fields.get(PAYMENT_KEY),
            fields.get(REASON),
            fields.get(ERROR_MESSAGE),
            LocalDateTime.parse(fields.get(FAILED_AT)),
            Integer.parseInt(fields.get(RETRY_COUNT))
        );
    }

    private HashOperations<String, String, String> hashOperations() {
        return stringRedisTemplate.opsForHash();
    }

    private String payloadKey(Long paymentId) {
        return PAYLOAD_KEY_PREFIX + paymentId;
    }

    private String member(Long paymentId) {
        return String.valueOf(paymentId);
    }

    private double toScore(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
