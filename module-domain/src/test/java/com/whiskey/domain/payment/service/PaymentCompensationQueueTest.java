package com.whiskey.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whiskey.domain.payment.dto.CompensatePaymentInfo;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

class PaymentCompensationQueueTest {

    private static final long PAYMENT_ID = 20L;
    private static final String MEMBER = "20";
    private static final String PAYLOAD_KEY = "payment:compensate:payload:20";
    private static final String SCHEDULE_KEY = "payment:compensate:schedule";
    private static final String DEAD_LETTER_KEY = "payment:compensate:dead";

    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);

    @SuppressWarnings("unchecked")
    private final HashOperations<String, String, String> hashOperations = mock(HashOperations.class);

    @SuppressWarnings("unchecked")
    private final ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);

    private final PaymentCompensationQueue queue = new PaymentCompensationQueue(stringRedisTemplate);

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    @DisplayName("적재 시 payload 는 Hash 에, 예정 시각은 스케줄 ZSet 에 paymentId 를 member 로 넣는다")
    void 적재_키구조() {
        LocalDateTime retryAt = LocalDateTime.of(2026, 7, 26, 17, 0, 9);

        queue.save(info(0, "Deadlock found when trying to get lock"), retryAt);

        verify(hashOperations).putAll(eq(PAYLOAD_KEY), anyMap());
        verify(zSetOperations).add(SCHEDULE_KEY, MEMBER, epochMillis(retryAt));
    }

    @Test
    @DisplayName("적재한 payload 를 그대로 복원한다")
    void 적재_조회_왕복() {
        CompensatePaymentInfo original = info(3, "Deadlock found when trying to get lock");

        queue.save(original, LocalDateTime.of(2026, 7, 26, 17, 0, 9));

        // 적재가 실제로 쓴 필드를 그대로 읽히게 해서, 적재와 조회가 역함수인지 확인한다.
        ArgumentCaptor<Map<String, String>> fields = ArgumentCaptor.captor();
        verify(hashOperations).putAll(eq(PAYLOAD_KEY), fields.capture());
        givenScheduled(MEMBER);
        when(hashOperations.entries(PAYLOAD_KEY)).thenReturn(fields.getValue());

        assertThat(queue.findDue(LocalDateTime.of(2026, 7, 26, 17, 5, 0))).containsExactly(original);
    }

    @Test
    @DisplayName("원인 메시지가 없으면 빈 문자열로 저장한다")
    void 원인메시지_null_처리() {
        // Hash 값에는 null 을 넣을 수 없다.
        queue.save(info(0, null), LocalDateTime.of(2026, 7, 26, 17, 0, 9));

        ArgumentCaptor<Map<String, String>> fields = ArgumentCaptor.captor();
        verify(hashOperations).putAll(eq(PAYLOAD_KEY), fields.capture());

        assertThat(fields.getValue().get("errorMessage")).isEmpty();
    }

    @Test
    @DisplayName("재예약은 member 를 바꾸지 않으므로 score 만 갱신한다")
    void 재예약_score만_갱신() {
        LocalDateTime nextRetryAt = LocalDateTime.of(2026, 7, 26, 17, 13, 5);

        queue.reschedule(PAYMENT_ID, nextRetryAt);

        verify(zSetOperations).add(SCHEDULE_KEY, MEMBER, epochMillis(nextRetryAt));
        // payload 를 member 에 넣던 구조에서는 여기서 remove + add 가 필요했고, 그걸 빠뜨려 갱신이 사라졌다.
        verify(zSetOperations, never()).remove(anyString(), any());
    }

    @Test
    @DisplayName("재시도 횟수는 HINCRBY 로 원자적으로 증가시킨다")
    void 재시도횟수_원자적_증가() {
        when(hashOperations.increment(PAYLOAD_KEY, "retryCount", 1)).thenReturn(3L);

        assertThat(queue.incrementRetryCount(PAYMENT_ID)).isEqualTo(3L);
    }

    @Test
    @DisplayName("제거 시 스케줄과 payload 를 모두 지운다")
    void 제거() {
        queue.remove(PAYMENT_ID);

        verify(zSetOperations).remove(SCHEDULE_KEY, MEMBER);
        verify(stringRedisTemplate).delete(PAYLOAD_KEY);
    }

    @Test
    @DisplayName("소진 항목은 dead 키로 옮기고 스케줄에서만 뺀다")
    void 소진_이관() {
        queue.moveToDeadLetter(PAYMENT_ID);

        verify(zSetOperations).add(eq(DEAD_LETTER_KEY), eq(MEMBER), anyDouble());
        verify(zSetOperations).remove(SCHEDULE_KEY, MEMBER);
        // 수동 처리에 쓰려면 payload 가 남아 있어야 한다.
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("스케줄에만 남고 payload 가 없는 항목은 정리하고 건너뛴다")
    void 고아_스케줄_정리() {
        givenScheduled(MEMBER);
        when(hashOperations.entries(PAYLOAD_KEY)).thenReturn(Map.of());

        assertThat(queue.findDue(LocalDateTime.of(2026, 7, 26, 17, 5, 0))).isEmpty();
        verify(zSetOperations).remove(SCHEDULE_KEY, MEMBER);
    }

    private void givenScheduled(String... paymentIds) {
        when(zSetOperations.rangeByScore(eq(SCHEDULE_KEY), eq(0d), anyDouble()))
            .thenReturn(new LinkedHashSet<>(List.of(paymentIds)));
    }

    private CompensatePaymentInfo info(int retryCount, String errorMessage) {
        return new CompensatePaymentInfo(
            PAYMENT_ID, 10L, "order-1", "mock_payment_key_order-1",
            "주문 확정 처리 실패", errorMessage,
            LocalDateTime.of(2026, 7, 26, 16, 55, 9, 79_279_800), retryCount
        );
    }

    private double epochMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
