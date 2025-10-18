package com.whiskey.domain.order.event.listener;

import com.whiskey.domain.order.event.OrderExpiryRegisteredEvent;
import com.whiskey.domain.order.service.OrderExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final OrderExpiryService orderExpiryService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handelOrderExpiryRegistered(OrderExpiryRegisteredEvent event) {
        orderExpiryService.reserveExpire(event.orderId(), event.expiresAt());
    }
}
