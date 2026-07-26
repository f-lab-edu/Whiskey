package com.whiskey.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiskey.domain.order.repository.OrderRepository;
import com.whiskey.domain.payment.dto.CompensatePaymentInfo;
import com.whiskey.domain.payment.repository.PaymentRepository;
import com.whiskey.payment.client.PaymentClient;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

class PaymentCompensationServiceTest {

    private static final String COMPENSATE_KEY = "payment:compensate";

    @Test
    @DisplayName("DLQ 적재 시 CompensatePaymentInfo 를 왕복 직렬화해 retryAt 스코어로 Redis ZSet 에 저장한다")
    @SuppressWarnings("unchecked")
    void DLQ적재_직렬화_및_스코어() {
        // 스프링이 실제로 주입하는 ObjectMapper 로 검증한다.
        // new ObjectMapper() 를 쓰면 JavaTimeModule 이 없어 failedAt(LocalDateTime) 에서 터진다.
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
            .run(context -> {
                ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

                StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
                ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
                when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

                PaymentCompensationService service = new PaymentCompensationService(
                    mock(PaymentClient.class),
                    mock(PaymentRepository.class),
                    mock(OrderRepository.class),
                    stringRedisTemplate,
                    objectMapper
                );

                CompensatePaymentInfo info = new CompensatePaymentInfo(
                    20L, 10L, "order-1", "pay-key",
                    "주문 확정 처리 실패", "Deadlock found when trying to get lock",
                    LocalDateTime.of(2026, 7, 26, 16, 33, 24), 0
                );
                LocalDateTime retryAt = LocalDateTime.of(2026, 7, 26, 16, 38, 24);

                service.compensateFailurePayment(info, retryAt);

                ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.captor();
                ArgumentCaptor<Double> scoreCaptor = ArgumentCaptor.captor();
                verify(zSetOperations).add(eq(COMPENSATE_KEY), jsonCaptor.capture(), scoreCaptor.capture());

                // RetryCheckScheduler 가 같은 매퍼로 읽어가므로 왕복이 되어야 재시도가 성립한다.
                assertThat(objectMapper.readValue(jsonCaptor.getValue(), CompensatePaymentInfo.class))
                    .isEqualTo(info);
                assertThat(scoreCaptor.getValue())
                    .isEqualTo(retryAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            });
    }
}
