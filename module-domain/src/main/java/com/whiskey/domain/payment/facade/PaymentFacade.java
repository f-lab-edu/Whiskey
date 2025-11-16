package com.whiskey.domain.payment.facade;

import com.whiskey.domain.order.Order;
import com.whiskey.domain.order.repository.OrderRepository;
import com.whiskey.domain.payment.Payment;
import com.whiskey.domain.payment.dto.PaymentConfirmCommand;
import com.whiskey.domain.payment.enums.PaymentStatus;
import com.whiskey.domain.payment.repository.PaymentRepository;
import com.whiskey.domain.payment.service.PaymentService;
import com.whiskey.payment.client.PaymentClient;
import com.whiskey.payment.dto.PaymentConfirmRequest;
import com.whiskey.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentFacade {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    private final PaymentClient paymentClient;
    private final PaymentService paymentService;

    // 결제 승인 요청
    public void confirmPayment(PaymentConfirmCommand command) {
        // 1. 결제와 주문 유효성 검사
        Payment payment = paymentService.validatePayment(command);
        Order order = orderRepository.findById(payment.getOrder().getId()).orElseThrow(() -> new IllegalStateException("주문을 찾을 수 없습니다."));

        // 2. PG 결제 승인 요청(트랜잭션 밖에서)
        PaymentResponse paymentResponse = requestPaymentConfirm(command);

        // 3. PG 결제 성공하면 DB 업데이트(트랜잭션 시작)
        paymentService.completePayment(payment, order, paymentResponse);
    }

    private PaymentResponse requestPaymentConfirm(PaymentConfirmCommand command) {
        try {
            PaymentConfirmRequest request = new PaymentConfirmRequest(
                command.paymentKey(),
                command.orderId(),
                command.amount().longValueExact()
            );

            return paymentClient.confirmPayment(request);

        } catch (Exception e) {
            log.error("PG 결제 승인 실패 - orderId: {}", command.orderId(), e);
            throw new RuntimeException("결제 승인에 실패했습니다: " + e.getMessage(), e);
        }
    }
}
