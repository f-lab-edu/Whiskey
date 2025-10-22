package com.whiskey.domain.payment.facade;

import com.whiskey.domain.order.repository.OrderRepository;
import com.whiskey.domain.payment.Payment;
import com.whiskey.domain.payment.dto.PaymentConfirmCommand;
import com.whiskey.domain.payment.enums.PaymentStatus;
import com.whiskey.domain.payment.repository.PaymentRepository;
import com.whiskey.payment.client.PaymentClient;
import com.whiskey.payment.dto.PaymentConfirmRequest;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentFacade {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;

    public void confirmPayment(PaymentConfirmCommand command) {
        Payment payment = validatePayment(command);

        try {
            PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentKey(command.paymentKey())

                .build();
        }
        catch (Exception e) {

        }
    }

    private Payment validatePayment(PaymentConfirmCommand command) {
        Payment payment = paymentRepository.findByPaymentOrderId(command.orderId());

        if(!payment.getMember().getId().equals(command.memberId())) {
            throw new IllegalArgumentException("memberId가 다릅니다.");
        }

        if(!payment.getAmount().equals(command.amount())) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        if(payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("결제 대기 중인 주문만 결제 승인이 가능합니다.");
        }

        return payment;
    }
}
