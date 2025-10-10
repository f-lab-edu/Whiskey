package com.whiskey.domain.payment.service;

import com.whiskey.domain.member.Member;
import com.whiskey.domain.payment.Payment;
import com.whiskey.domain.payment.dto.PaymentInfo;
import com.whiskey.domain.payment.repository.PaymentRepository;
import com.whiskey.payment.service.TossPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentService tossPaymentService;

    public Payment createPayment(Member member, Long amount, String description) {
        Payment payment = Payment.builder()
            .member(member)
            .amount(amount)
            .description(description)
            .build();

        return paymentRepository.save(payment);
    }
}
