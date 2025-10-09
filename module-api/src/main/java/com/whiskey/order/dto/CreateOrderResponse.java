package com.whiskey.order.dto;

import com.whiskey.domain.order.dto.OrderResult;

public record CreateOrderResponse(
    Long orderId
) {

    public static CreateOrderResponse from(OrderResult result) {
        return new CreateOrderResponse(result.orderId());
    }
}