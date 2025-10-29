package com.whiskey.domain.payment.service;

import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.repository.MemberRepository;
import com.whiskey.domain.order.Order;
import com.whiskey.domain.order.enums.OrderStatus;
import com.whiskey.domain.order.repository.OrderRepository;
import com.whiskey.domain.payment.Payment;
import com.whiskey.domain.payment.dto.PaymentConfirmCommand;
import com.whiskey.domain.payment.dto.PaymentPrepareCommand;
import com.whiskey.domain.payment.dto.PaymentPrepareResult;
import com.whiskey.domain.payment.repository.PaymentRepository;
import com.whiskey.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCommandService {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public PaymentPrepareResult createPayment(PaymentPrepareCommand command) {
        // 1. 회원 확인
        Member member = checkExistMember(command.memberId());

        // 2. 주문 조회
        Order order = orderRepository.findById(command.orderId()).orElseThrow(() -> ErrorCode.NOT_FOUND.exception("주문을 찾을 수 없습니다."));

        // 3. 내 주문이 맞는지 확인
        if(!order.getMemberId().equals(member.getId())) {
            throw new IllegalArgumentException("내 주문만 결제할 수 있습니다.");
        }

        // 4. 주문 상태 확인
        if(order.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("결제 대기 중인 주문이 아닙니다.");
        }

        if(order.getTotalPrice().compareTo(command.amount()) != 0) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        if(paymentRepository.existsByOrder(order) || order.getOrderStatus() == OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("이미 결제된 주문입니다.");
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

    private Member checkExistMember(long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> ErrorCode.NOT_FOUND.exception("존재하지 않는 회원입니다."));
    }
}
