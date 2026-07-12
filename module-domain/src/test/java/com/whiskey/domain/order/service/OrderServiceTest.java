package com.whiskey.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.whiskey.domain.order.Order;
import com.whiskey.domain.order.repository.OrderRepository;
import com.whiskey.domain.payment.repository.PaymentRepository;
import com.whiskey.domain.stock.repository.StockRepository;
import com.whiskey.domain.stock.service.StockReservationService;
import com.whiskey.exception.BusinessException;
import com.whiskey.exception.CommonErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

class OrderServiceTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderService orderService = new OrderService(
        orderRepository,
        mock(StockRepository.class),
        mock(PaymentRepository.class),
        mock(StockReservationService.class),
        mock(OrderExpiryService.class),
        mock(ApplicationEventPublisher.class)
    );

    @Test
    @DisplayName("본인 주문이 아니면 취소 시 인가 실패(FORBIDDEN, 403)")
    void cancelOrder_다른회원_FORBIDDEN() {
        Order order = Order.of(1L, new BigDecimal("100"), LocalDateTime.now().plusMinutes(10));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(10L, 2L))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> {
                BusinessException be = (BusinessException) ex;
                assertThat(be.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN);
                assertThat(be.getErrorCode().getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            });
    }
}
