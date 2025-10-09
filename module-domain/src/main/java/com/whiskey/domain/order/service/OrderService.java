package com.whiskey.domain.order.service;

import com.whiskey.domain.order.Order;
import com.whiskey.domain.order.dto.OrderCommand;
import com.whiskey.domain.order.dto.OrderCommand.OrderItem;
import com.whiskey.domain.order.dto.OrderResult;
import com.whiskey.domain.order.repository.OrderRepository;
import com.whiskey.domain.stock.repository.StockRepository;
import com.whiskey.domain.stock.Stock;
import com.whiskey.domain.stock.service.StockReservationService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;

    private final StockReservationService stockReservationService;

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
        Order order = Order.of(command.memberId(), totalPrice, expireTime);
        orderRepository.save(order);

        // 재고 예약
        for(OrderCommand.OrderItem item : command.items()) {
            Stock stock = findStockById(stocks, item.stockId());
            stockReservationService.reserveStock(order, stock, item.quantity(), expireTime);
        }

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
}