package com.whiskey.domain.payment;

import com.whiskey.domain.base.BaseEntity;
import com.whiskey.domain.member.Member;
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

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private String paymentKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column
    private String description;

    @Column(name = "request_date", nullable = false, updatable = false)
    private LocalDateTime requestDate;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Builder
    public Payment(Member member, Long amount, String description) {
        this.orderId = UUID.randomUUID().toString();
        this.paymentKey = null;
        this.member = member;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.description = description;
        this.requestDate = LocalDateTime.now();
        this.approvedDate = null;
    }
}