package com.whiskey.domain.stock;

import com.whiskey.domain.base.BaseEntity;
import com.whiskey.domain.stock.enums.StockStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseEntity {
    @Column(nullable = false)
    private long whiskeyId;

    @Column(nullable = false)
    private String batchVersion;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockStatus stockStatus;

    @Version
    private Long version;

    public static Stock of(Long whiskeyId, String batchVersion, int quantity, BigDecimal price) {
        Stock stock = new Stock();
        stock.whiskeyId = whiskeyId;
        stock.batchVersion = batchVersion;
        stock.quantity = quantity;
        stock.availableQuantity = quantity;
        stock.price = price;
        stock.stockStatus = quantity > 0 ? StockStatus.IN_STOCK : StockStatus.SOLD_OUT;
        return stock;
    }

    // 주문 예약
    public void reserve(int reserveQuantity) {
        checkReservation(reserveQuantity);
        this.availableQuantity -= reserveQuantity;
        updateStatus();
    }

    // 주문 취소
    public void cancel(int reserveQuantity) {
        this.availableQuantity += reserveQuantity;
        updateStatus();
    }

    // 주문 확정
    public void confirm(int reserveQuantity) {
        checkConfirm(reserveQuantity);
        this.quantity -= reserveQuantity;
        updateStatus();
    }

    // 재고상태 자동 업데이트
    private void updateStatus() {
        if(this.availableQuantity > 0) {
            this.stockStatus = StockStatus.IN_STOCK;
        }
        else {
            this.stockStatus = StockStatus.SOLD_OUT;
        }
    }

    // 주문 예약 확인
    private void checkReservation(int reserveQuantity) {
        if(reserveQuantity <= 0) {
            throw new IllegalArgumentException("주문 수량은 0보다 커야합니다.");
        }

        if(this.availableQuantity < reserveQuantity) {
            throw new IllegalArgumentException(String.format("재고가 부족합니다. (주문 : %d, 재고 : %d)", reserveQuantity, availableQuantity));
        }
    }

    // 주문 확정
    private void checkConfirm(int reserveQuantity) {
        if(reserveQuantity <= 0) {
            throw new IllegalArgumentException("확정주문 수량은 0보다 커야합니다.");
        }

        if(this.quantity < reserveQuantity) {
            throw new IllegalArgumentException(String.format("재고가 부족합니다. (주문 : %d, 재고 : %d)", reserveQuantity, quantity));
        }
    }
}