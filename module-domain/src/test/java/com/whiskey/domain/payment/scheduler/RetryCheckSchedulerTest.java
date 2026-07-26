package com.whiskey.domain.payment.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whiskey.domain.payment.dto.CompensatePaymentInfo;
import com.whiskey.domain.payment.service.PaymentCompensationQueue;
import com.whiskey.domain.payment.service.PaymentCompensationService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RetryCheckSchedulerTest {

    private static final long PAYMENT_ID = 20L;
    private static final int MAX_RETRY_COUNT = 5;

    private final PaymentCompensationQueue paymentCompensationQueue = mock(PaymentCompensationQueue.class);
    private final PaymentCompensationService paymentCompensationService = mock(PaymentCompensationService.class);

    private final RetryCheckScheduler scheduler =
        new RetryCheckScheduler(paymentCompensationQueue, paymentCompensationService);

    @Test
    @DisplayName("재시도가 성공하면 큐에서 제거한다")
    void 성공시_큐에서_제거() {
        givenDue(info(PAYMENT_ID, 0));

        scheduler.retryCheck();

        verify(paymentCompensationService).cancelPayment(any());
        // 지우지 않으면 매 틱마다 같은 보상을 반복 시도한다.
        verify(paymentCompensationQueue).remove(PAYMENT_ID);
        verify(paymentCompensationQueue, never()).reschedule(anyLong(), any());
    }

    @Test
    @DisplayName("재시도가 실패하면 횟수를 증가시키고 증가된 횟수만큼 뒤로 재예약한다")
    void 실패시_재예약() {
        givenDue(info(PAYMENT_ID, 0));
        givenCancelFails();
        when(paymentCompensationQueue.incrementRetryCount(PAYMENT_ID)).thenReturn(2L);

        LocalDateTime before = LocalDateTime.now();
        scheduler.retryCheck();

        verify(paymentCompensationQueue).incrementRetryCount(PAYMENT_ID);

        ArgumentCaptor<LocalDateTime> nextRetryAt = ArgumentCaptor.captor();
        verify(paymentCompensationQueue).reschedule(eq(PAYMENT_ID), nextRetryAt.capture());
        verify(paymentCompensationQueue, never()).remove(anyLong());

        // 2회차이므로 10분 뒤. 횟수가 누적되지 않으면 백오프가 자라지 않는다.
        assertThat(nextRetryAt.getValue()).isAfterOrEqualTo(before.plusMinutes(10));
    }

    @Test
    @DisplayName("재시도 횟수를 소진하면 dead-letter 로 옮기고 재예약하지 않는다")
    void 소진시_deadletter_이동() {
        givenDue(info(PAYMENT_ID, 4));
        givenCancelFails();
        when(paymentCompensationQueue.incrementRetryCount(PAYMENT_ID)).thenReturn((long) MAX_RETRY_COUNT);

        scheduler.retryCheck();

        verify(paymentCompensationQueue).moveToDeadLetter(PAYMENT_ID);
        verify(paymentCompensationQueue, never()).reschedule(anyLong(), any());
        verify(paymentCompensationQueue, never()).remove(anyLong());
    }

    @Test
    @DisplayName("한 항목 처리가 터져도 나머지 항목은 처리한다")
    void 한건_실패시_나머지_처리() {
        givenDue(info(PAYMENT_ID, 0), info(21L, 0));
        // 첫 항목은 큐 갱신 자체가 터진다 (Redis 장애 등).
        doThrow(new RuntimeException("Redis 접속 실패")).when(paymentCompensationQueue).remove(PAYMENT_ID);

        scheduler.retryCheck();

        verify(paymentCompensationService, times(2)).cancelPayment(any());
        verify(paymentCompensationQueue).remove(21L);
    }

    private void givenDue(CompensatePaymentInfo... infos) {
        when(paymentCompensationQueue.findDue(any())).thenReturn(List.of(infos));
    }

    private void givenCancelFails() {
        doThrow(new RuntimeException("PG 취소 실패")).when(paymentCompensationService).cancelPayment(any());
    }

    private CompensatePaymentInfo info(long paymentId, int retryCount) {
        return new CompensatePaymentInfo(
            paymentId, 10L, "order-" + paymentId, "mock_payment_key_order-" + paymentId,
            "주문 확정 처리 실패", "Deadlock found when trying to get lock",
            LocalDateTime.of(2026, 7, 26, 16, 55, 9), retryCount
        );
    }
}
