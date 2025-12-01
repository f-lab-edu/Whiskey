package com.whiskey.domain.stock;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.whiskey.domain.order.Order;
import com.whiskey.domain.stock.enums.StockStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class StockTest {
    @Test
    void 주문_예약_성공() {
        Stock stock = Stock.of(1L, 10, new BigDecimal("100"));

        stock.reserve(5);

        assertThat(stock.getAvailableQuantity()).isEqualTo(5);
        assertThat(stock.getQuantity()).isEqualTo(10);
        assertThat(stock.getStockStatus()).isEqualTo(StockStatus.IN_STOCK);
    }

    @Test
    void 재고_부족시_예외_발생() {
        Stock stock = Stock.of(1L,10, new BigDecimal("100"));

        assertThatThrownBy(() -> stock.reserve(15))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("재고가 부족합니다");
    }

    @Test
    void 예약_확정_후_재고_감소() {
        Stock stock = Stock.of(1L,10, new BigDecimal("100"));
        stock.reserve(5);

        stock.confirm(5);

        assertThat(stock.getQuantity()).isEqualTo(5);
        assertThat(stock.getAvailableQuantity()).isEqualTo(5);
    }

    @Test
    void 확정된_예약은_취소_불가() {
        Stock stock = Stock.of(1L,10, new BigDecimal("100"));
        Order order = Order.of(1L, new BigDecimal("100"), LocalDateTime.now().plusMinutes(10));
        StockReservation reservation = StockReservation.create(stock, order, 5, LocalDateTime.now().plusMinutes(10));
        reservation.confirm();

        assertThatThrownBy(() -> reservation.cancelByUser())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("이미 확정된 예약은 취소할 수 없습니다");
    }

    @Test
    void 전체_재고_예약시_SOLD_OUT() {
        Stock stock = Stock.of(1L, 10, new BigDecimal("100"));

        stock.reserve(10);

        assertThat(stock.getStockStatus()).isEqualTo(StockStatus.SOLD_OUT);
    }

    @Test
    void 예약_취소시_IN_STOCK_복구() {
        Stock stock = Stock.of(1L, 10, new BigDecimal("100"));
        stock.reserve(10);

        stock.cancel(10);

        assertThat(stock.getStockStatus()).isEqualTo(StockStatus.IN_STOCK);
    }

    @Test
    void 만료된_예약은_확정_불가() {
        Stock stock = Stock.of(1L, 10, new BigDecimal("100"));
        Order order = Order.of(1L, new BigDecimal("100"), LocalDateTime.now().plusMinutes(10));
        StockReservation reservation = StockReservation.create(stock, order, 5, LocalDateTime.now().minusMinutes(10)); // 이미 만료

        assertThatThrownBy(() -> reservation.confirm())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("만료된 예약은 주문 확정할 수 없습니다.");
    }
}