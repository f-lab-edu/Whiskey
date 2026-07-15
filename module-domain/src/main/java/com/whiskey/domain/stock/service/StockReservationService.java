package com.whiskey.domain.stock.service;

import com.whiskey.domain.order.Order;
import com.whiskey.domain.stock.Stock;
import com.whiskey.domain.stock.StockReservation;
import com.whiskey.domain.stock.repository.StockRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockReservationService {

    private final StockRepository stockRepository;

    // 낙관락 충돌 재시도는 트랜잭션 경계(OrderService.createOrder)에서 처리한다.
    // 이 메서드는 바깥 트랜잭션에 합류하며, 버전 충돌은 커밋(flush) 시점에 발생한다.
    public void reserveStock(Order order, Stock stock, int quantity, LocalDateTime expireAt) {
        Stock freshStock = stockRepository.findById(stock.getId())
            .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다."));

        StockReservation.create(freshStock, order, quantity, expireAt);
        stockRepository.save(freshStock);
    }
}
