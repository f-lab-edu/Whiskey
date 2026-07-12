package com.whiskey.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.whiskey.exception.BusinessException;
import com.whiskey.exception.CommonErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class OrderTest {

    private static final BigDecimal PRICE = new BigDecimal("100");

    @Test
    @DisplayName("본인 주문이 아니면 결제 검증 시 인가 실패(FORBIDDEN, 403)")
    void validatePayment_다른회원_FORBIDDEN() {
        Order order = Order.of(1L, PRICE, LocalDateTime.now().plusMinutes(10));

        assertThatThrownBy(() -> order.validatePayment(2L, PRICE))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> {
                BusinessException be = (BusinessException) ex;
                assertThat(be.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN);
                assertThat(be.getErrorCode().getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            });
    }

    @Test
    @DisplayName("본인 주문이면 소유권 검증을 통과한다")
    void validatePayment_본인_통과() {
        Order order = Order.of(1L, PRICE, LocalDateTime.now().plusMinutes(10));

        assertThatCode(() -> order.validatePayment(1L, PRICE))
            .doesNotThrowAnyException();
    }
}
