package com.whiskey.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.service.MemberService;
import com.whiskey.domain.order.service.OrderService;
import com.whiskey.domain.payment.Payment;
import com.whiskey.domain.payment.dto.PaymentConfirmCommand;
import com.whiskey.domain.payment.repository.PaymentRepository;
import com.whiskey.exception.BusinessException;
import com.whiskey.exception.CommonErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PaymentServiceTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final PaymentService paymentService = new PaymentService(
        paymentRepository,
        mock(OrderService.class),
        mock(MemberService.class)
    );

    @Test
    @DisplayName("본인 결제가 아니면 승인 검증 시 인가 실패(FORBIDDEN, 403)")
    void validatePayment_다른회원_FORBIDDEN() {
        Member owner = mock(Member.class);
        when(owner.getId()).thenReturn(1L);
        Payment payment = mock(Payment.class);
        when(payment.getMember()).thenReturn(owner);
        when(paymentRepository.findByPaymentOrderId("order-1")).thenReturn(Optional.of(payment));

        PaymentConfirmCommand command = new PaymentConfirmCommand(2L, "order-1", 1000L, "pay-key");

        assertThatThrownBy(() -> paymentService.validatePayment(command))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> {
                BusinessException be = (BusinessException) ex;
                assertThat(be.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN);
                assertThat(be.getErrorCode().getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            });
    }
}
