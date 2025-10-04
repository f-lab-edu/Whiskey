package com.whiskey.domain.order.dto;

import com.whiskey.domain.order.enums.OrderStatusType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResult(
    Long orderId,
    BigDecimal totalPrice,
    LocalDateTime expireAt,
    OrderStatusType orderStatus
) {}