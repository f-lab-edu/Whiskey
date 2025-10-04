package com.whiskey.domain.stock;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.whiskey.domain.stock.enums.StatusType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StockTest {
    @Test
    void 주문_예약_성공() {
        Stock stock = Stock.of(1L, "v1", 10, new BigDecimal("100"));

        stock.reserve(5);

        assertThat(stock.getAvailableQuantity()).isEqualTo(5);
        assertThat(stock.getQuantity()).isEqualTo(10);
        assertThat(stock.getStatusType()).isEqualTo(StatusType.IN_STOCK);
    }

    @Test
    void 재고_부족시_예외_발생() {
        Stock stock = Stock.of(1L, "v1", 10, new BigDecimal("100"));

        assertThatThrownBy(() -> stock.reserve(15))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("재고가 부족합니다");
    }

    @Test
    void 예약_확정_후_재고_감소() {
        Stock stock = Stock.of(1L, "v1", 10, new BigDecimal("100"));
        stock.reserve(5);

        stock.confirm(5);

        assertThat(stock.getQuantity()).isEqualTo(5);
        assertThat(stock.getAvailableQuantity()).isEqualTo(5);
    }

    @Test
    void 확정된_예약은_취소_불가() {
        Stock stock = Stock.of(1L, "v1", 10, new BigDecimal("100"));
        StockReservation reservation = StockReservation.create(stock, 123L, 5, 10);
        reservation.confirm();

        assertThatThrownBy(() -> reservation.cancelByUser())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("이미 확정된 예약은 취소할 수 없습니다");
    }

    @Test
    void 전체_재고_예약시_SOLD_OUT() {
        Stock stock = Stock.of(1L, "v1", 10, new BigDecimal("100"));

        stock.reserve(10);

        assertThat(stock.getStatusType()).isEqualTo(StatusType.SOLD_OUT);
    }

    @Test
    void 예약_취소시_IN_STOCK_복구() {
        Stock stock = Stock.of(1L, "v1", 10, new BigDecimal("100"));
        stock.reserve(10);

        stock.cancel(10);

        assertThat(stock.getStatusType()).isEqualTo(StatusType.IN_STOCK);
    }

    @Test
    void 만료된_예약은_확정_불가() {
        Stock stock = Stock.of(1L, "v1", 10, new BigDecimal("100"));
        StockReservation reservation = StockReservation.create(stock, 123L, 5, -1); // 이미 만료

        assertThatThrownBy(() -> reservation.confirm())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("예약 상태인 주문만 확정할 수 있습니다.");
    }
}