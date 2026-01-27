package com.whiskey.domain.payment;

import com.whiskey.domain.base.BaseEntity;
import com.whiskey.domain.member.Member;
import com.whiskey.domain.order.Order;
import com.whiskey.domain.payment.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String paymentOrderId;

    @Column
    private String paymentKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Column
    private String description;

    @Column(name = "request_date", nullable = false, updatable = false)
    private LocalDateTime requestDate;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Builder
    public Payment(Member member, Order order, Long amount, String description) {
        this.paymentOrderId = UUID.randomUUID().toString();
        this.paymentKey = null;
        this.member = member;
        this.order = order;
        this.amount = amount;
        this.paymentStatus = PaymentStatus.PENDING;
        this.description = description;
        this.requestDate = LocalDateTime.now();
        this.approvedDate = null;
    }

    public void expirePayment() {
        if(this.paymentStatus != PaymentStatus.PENDING) {
            throw new IllegalStateException("대기 상태에서만 결제 취소가 가능합니다.");
        }

        this.paymentStatus = PaymentStatus.EXPIRED;
    }

    public void completePayment(String paymentKey) {
        if(this.paymentStatus != PaymentStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 완료 처리가 가능합니다.");
        }

        this.paymentStatus = PaymentStatus.COMPLETED;
        this.paymentKey = paymentKey;
        this.approvedDate = LocalDateTime.now();
    }

    public void cancelPayment() {
        if(this.paymentStatus != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("완료 상태인 결제만 취소 처리가 가능합니다.");
        }

        this.paymentStatus = PaymentStatus.CANCELLED;
    }
}