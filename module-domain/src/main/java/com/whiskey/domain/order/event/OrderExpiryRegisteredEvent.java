package com.whiskey.domain.order.event;

import java.time.LocalDateTime;

public record OrderExpiryRegisteredEvent(
    Long orderId,
    LocalDateTime expiresAt
) {}