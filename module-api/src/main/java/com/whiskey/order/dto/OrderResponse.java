package com.whiskey.order.dto;

import com.whiskey.domain.order.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long orderId,
    Long memberId,
    BigDecimal totalPrice,
    OrderStatus orderStatus,
    String paymentId,
    LocalDateTime createdAt,
    LocalDateTime expireAt,
    List<ReservationItem> items
) {
    public record ReservationItem(
        Long stockId,
        int quantity,
        BigDecimal price
    ) {}
}