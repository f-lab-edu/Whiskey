package com.whiskey.domain.payment.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.whiskey.domain.payment.dto.CompensatePaymentInfo;
import com.whiskey.domain.payment.service.PaymentCompensationService;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

class RetryCheckSchedulerTest {

    private static final String COMPENSATE_KEY = "payment:compensate";
    private static final String DEAD_LETTER_KEY = "payment:compensate:dead";
    private static final int MAX_RETRY_COUNT = 5;

    // 운영과 같은 구성(JavaTimeModule + ISO-8601).
    // 실제 주입되는 빈이 이 구성인지는 PaymentCompensationServiceTest 가 검증한다.
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();

    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    private final PaymentCompensationService paymentCompensationService = mock(PaymentCompensationService.class);

    @SuppressWarnings("unchecked")
    private final ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);

    private final RetryCheckScheduler scheduler =
        new RetryCheckScheduler(stringRedisTemplate, paymentCompensationService, OBJECT_MAPPER);

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    @DisplayName("재시도가 성공하면 재시도 큐에서 제거한다")
    void 성공시_큐에서_제거() throws Exception {
        String jsonData = json(info(0));
        givenQueued(jsonData);

        scheduler.retryCheck();

        verify(paymentCompensationService).cancelPayment(any());
        // 지우지 않으면 매 틱마다 같은 보상을 반복 시도한다.
        verify(zSetOperations).remove(COMPENSATE_KEY, jsonData);
        verify(zSetOperations, never()).add(eq(COMPENSATE_KEY), anyString(), anyDouble());
    }

    @Test
    @DisplayName("재시도가 실패하면 증가된 retryCount 를 직렬화해 다시 적재한다")
    void 실패시_retryCount_누적() throws Exception {
        CompensatePaymentInfo original = info(0);
        String jsonData = json(original);
        givenQueued(jsonData);
        doThrow(new RuntimeException("PG 취소 실패")).when(paymentCompensationService).cancelPayment(any());

        scheduler.retryCheck();

        ArgumentCaptor<String> reAdded = ArgumentCaptor.captor();
        verify(zSetOperations).add(eq(COMPENSATE_KEY), reAdded.capture(), anyDouble());
        verify(zSetOperations).remove(COMPENSATE_KEY, jsonData);

        // 원본 문자열을 재적재하면 retryCount 가 0에 머물러 소진 가드가 영원히 걸리지 않는다.
        assertThat(OBJECT_MAPPER.readValue(reAdded.getValue(), CompensatePaymentInfo.class))
            .isEqualTo(original.incrRetryCount());
    }

    @Test
    @DisplayName("재시도 5회를 소진하면 dead-letter 키로 옮기고 재시도 큐에서 제거한다")
    void 소진시_deadletter_이동() throws Exception {
        String jsonData = json(info(4));  // 이번 실패로 5회가 된다
        givenQueued(jsonData);
        doThrow(new RuntimeException("PG 취소 실패")).when(paymentCompensationService).cancelPayment(any());

        scheduler.retryCheck();

        ArgumentCaptor<String> deadLettered = ArgumentCaptor.captor();
        verify(zSetOperations).add(eq(DEAD_LETTER_KEY), deadLettered.capture(), anyDouble());
        verify(zSetOperations).remove(COMPENSATE_KEY, jsonData);
        verify(zSetOperations, never()).add(eq(COMPENSATE_KEY), anyString(), anyDouble());

        // 소진 로그의 횟수와 옮겨진 레코드가 일치해야 관리자가 혼동하지 않는다.
        assertThat(OBJECT_MAPPER.readValue(deadLettered.getValue(), CompensatePaymentInfo.class).retryCount())
            .isEqualTo(MAX_RETRY_COUNT);
    }

    @Test
    @DisplayName("한 항목이 역직렬화에 실패해도 나머지 항목은 처리한다")
    void 한건_실패시_나머지_처리() throws Exception {
        String broken = "{ not json";
        String valid = json(info(0));
        givenQueued(broken, valid);

        scheduler.retryCheck();

        verify(paymentCompensationService, times(1)).cancelPayment(any());
        verify(zSetOperations).remove(COMPENSATE_KEY, valid);
    }

    private void givenQueued(String... jsonData) {
        when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble()))
            .thenReturn(new LinkedHashSet<>(List.of(jsonData)));
    }

    private CompensatePaymentInfo info(int retryCount) {
        return new CompensatePaymentInfo(
            20L, 10L, "order-1", "mock_payment_key_order-1",
            "주문 확정 처리 실패", "Deadlock found when trying to get lock",
            LocalDateTime.of(2026, 7, 26, 16, 55, 9), retryCount
        );
    }

    private String json(CompensatePaymentInfo info) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(info);
    }
}
