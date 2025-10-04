package com.whiskey.domain.order.dto;

import java.util.List;

public record OrderCommand(
    Long memberId,
    List<OrderItem> items
) {
    public record OrderItem(
        Long stockId,
        int quantity
    ) {}
}