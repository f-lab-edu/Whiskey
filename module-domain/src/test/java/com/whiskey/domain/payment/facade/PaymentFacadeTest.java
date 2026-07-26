package com.whiskey.domain.payment.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.whiskey.domain.order.Order;
import com.whiskey.domain.order.exception.OrderProcessingException;
import com.whiskey.domain.order.service.OrderService;
import com.whiskey.domain.payment.Payment;
import com.whiskey.domain.payment.dto.PaymentConfirmCommand;
import com.whiskey.domain.payment.exception.PaymentProcessingException;
import com.whiskey.domain.payment.service.PaymentCompensationQueue;
import com.whiskey.domain.payment.service.PaymentCompensationService;
import com.whiskey.domain.payment.service.PaymentService;
import com.whiskey.payment.client.PaymentClient;
import com.whiskey.payment.dto.PaymentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PaymentFacadeTest {

    private static final PaymentConfirmCommand COMMAND =
        new PaymentConfirmCommand(1L, "order-1", 5000L, "pay-key");
    private static final long ORDER_ID = 10L;

    private final PaymentClient paymentClient = mock(PaymentClient.class);
    private final PaymentService paymentService = mock(PaymentService.class);
    private final OrderService orderService = mock(OrderService.class);
    private final PaymentCompensationService compensationService = mock(PaymentCompensationService.class);
    private final PaymentCompensationQueue compensationQueue = mock(PaymentCompensationQueue.class);

    @Test
    @DisplayName("결제 완료 처리 실패 후 DLQ 적재까지 실패해도 원인 예외가 덮이지 않는다")
    void DLQ적재실패_결제원인예외_보존() throws Exception {
        givenPgApproved();
        doThrow(new RuntimeException("Deadlock found when trying to get lock"))
            .when(paymentService).completePayment(any());
        givenCompensationAndDlqBothFail();

        // DLQ 적재 예외가 올라오면 호출부 catch 블록이 중단되어 PaymentProcessingException 이 던져지지 못한다.
        assertThatThrownBy(() -> facade().confirmPayment(COMMAND))
            .isInstanceOf(PaymentProcessingException.class)
            .hasMessage("결제 처리 중 오류가 발생했습니다.")
            .hasRootCauseMessage("Deadlock found when trying to get lock");
    }

    @Test
    @DisplayName("주문 확정 실패 후 DLQ 적재까지 실패해도 원인 예외가 덮이지 않는다")
    void DLQ적재실패_주문원인예외_보존() throws Exception {
        givenPgApproved();
        doThrow(new RuntimeException("Deadlock found when trying to get lock"))
            .when(orderService).confirmReservation(ORDER_ID);
        givenCompensationAndDlqBothFail();

        assertThatThrownBy(() -> facade().confirmPayment(COMMAND))
            .isInstanceOf(OrderProcessingException.class)
            .hasMessage("주문 처리 중 오류가 발생했습니다.")
            .hasRootCauseMessage("Deadlock found when trying to get lock");
    }

    private PaymentFacade facade() {
        PaymentFacade facade =
            new PaymentFacade(paymentClient, paymentService, orderService, compensationService, compensationQueue);
        ReflectionTestUtils.setField(facade, "retryTime", 5);
        return facade;
    }

    private void givenPgApproved() throws JsonProcessingException {
        Order order = mock(Order.class);
        when(order.getId()).thenReturn(ORDER_ID);

        Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(20L);
        when(payment.getPaymentOrderId()).thenReturn("order-1");
        when(payment.getOrder()).thenReturn(order);

        when(paymentService.validatePayment(any())).thenReturn(payment);
        when(paymentClient.confirmPayment(any())).thenReturn(new PaymentResponse("pay-key", "order-1", 5000L));
    }

    // 보상 트랜잭션도 실패하고, 그 후처리인 DLQ 적재까지 실패하는 최악의 상황
    private void givenCompensationAndDlqBothFail() {
        doThrow(new RuntimeException("PG 취소 실패"))
            .when(compensationService).cancelPayment(any());
        doThrow(new RuntimeException("Redis 접속 실패"))
            .when(compensationQueue).save(any(), any());
    }
}
