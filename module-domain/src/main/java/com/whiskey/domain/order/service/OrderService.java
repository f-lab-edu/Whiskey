package com.whiskey.domain.order.service;

import com.whiskey.domain.order.Order;
import com.whiskey.domain.order.dto.OrderCommand;
import com.whiskey.domain.order.dto.OrderCommand.OrderItem;
import com.whiskey.domain.order.dto.OrderResult;
import com.whiskey.domain.order.enums.OrderStatus;
import com.whiskey.domain.order.event.OrderExpiryRegisteredEvent;
import com.whiskey.domain.order.repository.OrderRepository;
import com.whiskey.domain.payment.Payment;
import com.whiskey.domain.payment.enums.PaymentStatus;
import com.whiskey.domain.payment.repository.PaymentRepository;
import com.whiskey.domain.stock.repository.StockRepository;
import com.whiskey.domain.stock.Stock;
import com.whiskey.domain.stock.service.StockReservationService;
import com.whiskey.exception.BusinessException;
import com.whiskey.exception.CommonErrorCode;
import com.whiskey.exception.OrderErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;
    private final PaymentRepository paymentRepository;

    private final StockReservationService stockReservationService;
    private final OrderExpiryService orderExpiryService;

    private final ApplicationEventPublisher eventPublisher;

    @Value("${order.reservation.expire-time:30}")
    private int expireTime;

    // 동일 재고에 동시 주문이 몰리면 낙관락 버전 충돌(ObjectOptimisticLockingFailureException) 또는
    // InnoDB 데드락/락 대기(CannotAcquireLockException)로 표면화된다.
    // 두 경우의 공통 상위인 ConcurrencyFailureException을 재시도 대상으로 잡아, 새 트랜잭션에서 전체를 원자적으로 재실행한다.
    // random 지터로 재시도가 같은 시점에 몰려 다시 충돌하는 lockstep을 완화한다.
    // (고경합 상황의 근본 해법은 별도 이슈 #65의 분산락 검토 대상)
    @Retryable(
        retryFor = ConcurrencyFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2, random = true)
    )
    @Transactional
    public OrderResult createOrder(OrderCommand command) {
        // 주문 생성 및 재고 예약
        List<Long> stockIds = command.items().stream()
            .map(OrderCommand.OrderItem::stockId)
            .toList();

        List<Stock> stocks = stockRepository.findAllById(stockIds);

        if (stocks.size() != stockIds.size()) {
            throw OrderErrorCode.INVALID_ORDER_ITEM.exception("재고가 없는 상품이 포함되어 있습니다.");
        }

        // 총 금액 계산
        BigDecimal totalPrice = calculateTotalPrice(command.items(), stocks);

        // Order 저장
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(expireTime);
        Order order = Order.of(command.memberId(), totalPrice, expireAt);
        orderRepository.save(order);

        // 재고 예약
        for(OrderCommand.OrderItem item : command.items()) {
            Stock stock = findStockById(stocks, item.stockId());
            stockReservationService.reserveStock(order, stock, item.quantity(), expireAt);
        }

        // 주문 만료시간 이벤트 발행
        eventPublisher.publishEvent(new OrderExpiryRegisteredEvent(order.getId(), expireAt));
        return new OrderResult(order.getId());
    }

    // 재시도(3회)를 모두 소진한 경우: 고경합으로 재고 예약 실패 → 409 응답
    @Recover
    public OrderResult recoverCreateOrder(ConcurrencyFailureException e, OrderCommand command) {
        log.warn("재고 예약 동시성 충돌 재시도 소진 - memberId: {}, cause: {}", command.memberId(), e.getClass().getSimpleName());
        throw OrderErrorCode.STOCK_RESERVATION_CONFLICT.exception();
    }

    private BigDecimal calculateTotalPrice(List<OrderItem> items, List<Stock> stocks) {
        return items.stream()
            .map(item -> {
                Stock stock = findStockById(stocks, item.stockId());
                return stock.getPrice().multiply(BigDecimal.valueOf(item.quantity()));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Stock findStockById(List<Stock> stocks, Long stockId) {
        return stocks.stream()
            .filter(stock -> stock.getId().equals(stockId))
            .findFirst()
            .orElseThrow(() -> OrderErrorCode.INVALID_ORDER_ITEM.exception("재고가 없는 상품이 포함되어 있습니다. stockId: " + stockId));
    }

    @Transactional
    public void cancelOrder(Long orderId, Long memberId) {
        Order order = getOrder(orderId);

        if(!order.getMemberId().equals(memberId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "본인의 주문만 취소가 가능합니다.");
        }

        order.cancelReservation();

        orderExpiryService.removeExpire(orderId);
    }

    @Transactional
    public void expireOrder(long orderId) {
        Order order = getOrder(orderId);

        // PENDING 주문만 만료 처리
        if(order.getOrderStatus() != OrderStatus.PENDING) {
            return;
        }

        order.expireReservation();

        // 연관된 PENDING 상태 결제도 만료 처리
        List<Payment> payments = paymentRepository.findByOrderAndPaymentStatus(order, PaymentStatus.PENDING);
        if(!payments.isEmpty()) {
            payments.forEach(Payment::expirePayment);
        }
    }

    @Transactional
    public void confirmReservation(long orderId) {
        Order order = getOrder(orderId);
        order.confirmReservation();
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> OrderErrorCode.ORDER_NOT_FOUND.exception("존재하지 않는 주문입니다."));
    }
}