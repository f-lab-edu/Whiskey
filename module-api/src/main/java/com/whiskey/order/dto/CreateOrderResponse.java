package com.whiskey.order.dto;

import com.whiskey.domain.order.enums.OrderStatusType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateOrderResponse(
    Long orderId,
    BigDecimal totalPrice,
    LocalDateTime expireAt,
    OrderStatusType orderStatus
) {}