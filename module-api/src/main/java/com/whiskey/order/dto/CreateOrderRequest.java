package com.whiskey.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record CreateOrderRequest(
    @NotEmpty(message = "주문 항목은 최소 1개 이상이어야 합니다.")
    @Valid
    List<OrderItem> items
) {
    public record OrderItem(
        @NotNull(message = "재고 ID는 필수입니다.")
        Long stockId,

        @Positive(message = "수량은 1개 이상이어야 합니다.")
        int quantity
    ) {}
}