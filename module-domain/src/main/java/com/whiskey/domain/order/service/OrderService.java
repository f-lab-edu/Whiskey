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
import com.whiskey.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
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

    @Transactional
    public OrderResult createOrder(OrderCommand command) {
        // 주문 생성 및 재고 예약
        List<Long> stockIds = command.items().stream()
            .map(OrderCommand.OrderItem::stockId)
            .toList();

        List<Stock> stocks = stockRepository.findAllById(stockIds);

        if (stocks.size() != stockIds.size()) {
            throw new IllegalArgumentException("재고가 없는 상품이 포함되어 있습니다.");
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
            .orElseThrow(() -> new IllegalArgumentException("재고가 없는 상품이 포함되어 있습니다. stockId: " + stockId));
    }

    @Transactional
    public void cancelOrder(Long orderId, Long memberId) {
        Order order = getOrder(orderId);

        if(!order.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 주문만 취소가 가능합니다.");
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
        return orderRepository.findById(orderId).orElseThrow(() -> ErrorCode.NOT_FOUND.exception("존재하지 않는 주문입니다."));
    }
}