package com.whiskey.domain.order;

import com.whiskey.domain.base.BaseEntity;
import com.whiskey.domain.order.enums.OrderStatus;
import com.whiskey.domain.stock.StockReservation;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Column(nullable = false)
    private Long memberId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<StockReservation> reservations = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    private String paymentId;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public static Order of(Long memberId, BigDecimal totalPrice, LocalDateTime expireMinutes) {
        Order order = new Order();
        order.memberId = memberId;
        order.totalPrice = totalPrice;
        order.orderStatus = OrderStatus.PENDING;
        order.expireAt = expireMinutes;
        return order;
    }

    public void addReservation(StockReservation reservation) {
        this.reservations.add(reservation);
    }

    public void confirmReservation(String paymentId) {
        checkConfirm();
        this.orderStatus = OrderStatus.CONFIRMED;
        this.paymentId = paymentId;
        this.confirmedAt = LocalDateTime.now();

        reservations.forEach(StockReservation::confirm);
    }

    public void cancelReservation() {
        checkCancel();
        this.orderStatus = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();

        reservations.forEach(StockReservation::cancelByUser);
    }

    public void expireReservation() {
        this.orderStatus = OrderStatus.EXPIRED;
        this.cancelledAt = LocalDateTime.now();

        reservations.forEach(StockReservation::cancelByExpiration);
    }

    private void checkConfirm() {
        if(this.orderStatus != OrderStatus.PENDING) {
            throw new IllegalStateException("대기 상태에서만 주문 확정이 가능합니다.");
        }

        if(LocalDateTime.now().isAfter(expireAt)) {
            throw new IllegalStateException("만료된 주문은 확정할 수 없습니다.");
        }
    }

    private void checkCancel() {
        if(this.orderStatus == OrderStatus.CANCELLED || this.orderStatus == OrderStatus.EXPIRED) {
            throw new IllegalStateException("이미 취소된 주문입니다.");
        }
    }
}
