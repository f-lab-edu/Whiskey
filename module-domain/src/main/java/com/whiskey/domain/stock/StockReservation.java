package com.whiskey.domain.stock;

import com.whiskey.domain.base.BaseEntity;
import com.whiskey.domain.order.Order;
import com.whiskey.domain.stock.enums.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReservation extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private int reservedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus reservationStatus;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public static StockReservation create(Stock stock, Order order, int quantity, LocalDateTime expireAt) {
        StockReservation reservation = new StockReservation();
        reservation.stock = stock;
        reservation.order = order;
        reservation.reservedQuantity = quantity;
        reservation.reservationStatus = ReservationStatus.RESERVED;
        reservation.expireAt = expireAt;

        stock.reserve(quantity);
        order.addReservation(reservation);
        return reservation;
    }

    public void confirm() {
        checkConfirm();
        this.reservationStatus = ReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();

        // Stock의 실제 재고 차감
        this.stock.confirm(this.reservedQuantity);
    }

    public void cancelByUser() {
        if(this.reservationStatus == ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("이미 확정된 예약은 취소할 수 없습니다.");
        }

        this.reservationStatus = ReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();

        // Stock의 실제 재고 추가
        this.stock.cancel(this.reservedQuantity);
    }

    public void cancelByExpiration() {
        this.reservationStatus = ReservationStatus.EXPIRED;
        this.cancelledAt = LocalDateTime.now();

        // Stock의 실제 재고 추가
        this.stock.cancel(this.reservedQuantity);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expireAt) && this.reservationStatus == ReservationStatus.RESERVED;
    }

    private void checkConfirm() {
        if(this.reservationStatus != ReservationStatus.RESERVED) {
            throw new IllegalStateException("예약 상태인 주문만 확정할 수 있습니다.");
        }

        if(isExpired()) {
            throw new IllegalStateException("만료된 예약은 주문 확정할 수 없습니다.");
        }
    }
}