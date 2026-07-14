package com.whiskey.domain.payment.service;

import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.service.MemberService;
import com.whiskey.domain.order.Order;
import com.whiskey.domain.order.enums.OrderStatus;
import com.whiskey.domain.order.service.OrderService;
import com.whiskey.domain.payment.Payment;
import com.whiskey.domain.payment.dto.PaymentCompleteRequest;
import com.whiskey.domain.payment.dto.PaymentConfirmCommand;
import com.whiskey.domain.payment.dto.PaymentPrepareCommand;
import com.whiskey.domain.payment.dto.PaymentPrepareResult;
import com.whiskey.domain.payment.enums.PaymentStatus;
import com.whiskey.domain.payment.repository.PaymentRepository;
import com.whiskey.exception.BusinessException;
import com.whiskey.exception.CommonErrorCode;
import com.whiskey.exception.OrderErrorCode;
import com.whiskey.exception.PaymentErrorCode;
import com.whiskey.payment.client.PaymentClient;
import com.whiskey.payment.dto.PaymentResponse;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final OrderService orderService;
    private final MemberService memberService;

    @Transactional
    public PaymentPrepareResult createPayment(PaymentPrepareCommand command) {
        // 1. 회원 확인
        Member member = memberService.getMember(command.memberId());

        // 2. 주문 조회 및 체크
        Order order = orderService.getOrder(command.orderId());
        order.validatePayment(member.getId(), order.getTotalPrice());

        // 3. 내 주문이 맞는지 확인
        if(!order.getMemberId().equals(member.getId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "내 주문만 결제할 수 있습니다.");
        }

        // 4. 주문 상태 확인
        if(order.getOrderStatus() != OrderStatus.PENDING) {
            throw OrderErrorCode.INVALID_ORDER_STATUS.exception("결제 대기 중인 주문이 아닙니다.");
        }

        if(order.getTotalPrice().compareTo(BigDecimal.valueOf(command.amount())) != 0) {
            throw PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH.exception("결제 금액이 일치하지 않습니다.");
        }

        if(paymentRepository.existsByOrder(order) || order.getOrderStatus() == OrderStatus.CONFIRMED) {
            throw PaymentErrorCode.ORDER_ALREADY_PAID.exception("이미 결제된 주문입니다.");
        }

        Payment payment = Payment.builder()
            .member(member)
            .order(order)
            .amount(command.amount())
            .description(command.description())
            .build();

        paymentRepository.save(payment);
        return new PaymentPrepareResult(payment.getPaymentOrderId(), payment.getAmount());
    }

    @Transactional
    public void completePayment(PaymentCompleteRequest request) {
        Payment payment = paymentRepository.findByPaymentOrderId(request.paymentOrderId()).orElseThrow(() -> PaymentErrorCode.PAYMENT_NOT_FOUND.exception("결제 정보를 찾을 수 없습니다"));

        // 비즈니스 로직에만 집중
        // 예외 발생시 트랜잭션 롤백, PaymentFacade로 던져짐
        payment.completePayment(payment.getPaymentKey());
    }

    @Transactional
    public Payment validatePayment(PaymentConfirmCommand command) {
        Payment payment = paymentRepository.findByPaymentOrderId(command.orderId()).orElseThrow(() -> PaymentErrorCode.PAYMENT_NOT_FOUND.exception("결제 정보를 찾을 수 없습니다"));

        if(!payment.getMember().getId().equals(command.memberId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "본인의 결제만 승인할 수 있습니다.");
        }

        if(!payment.getAmount().equals(command.amount())) {
            throw PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH.exception("결제 금액이 일치하지 않습니다.");
        }

        if(payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw PaymentErrorCode.INVALID_PAYMENT_STATUS.exception("결제 대기 중인 주문만 결제 승인이 가능합니다.");
        }

        return payment;
    }
}
