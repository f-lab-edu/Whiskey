package com.whiskey.domain.payment.dto;

import java.time.LocalDateTime;

// retryCount 는 PaymentCompensationQueue 가 HINCRBY 로 원자적으로 증가시킨다.
// 여기서 증가된 복사본을 만들면 저장을 빠뜨리기 쉬워(실제로 그런 버그가 있었다) 의도적으로 두지 않는다.
public record CompensatePaymentInfo(
    Long paymentId,
    Long orderId,
    String paymentOrderId,
    String paymentKey,
    String reason,
    String errorMessage,
    LocalDateTime failedAt,
    int retryCount
) {
}
