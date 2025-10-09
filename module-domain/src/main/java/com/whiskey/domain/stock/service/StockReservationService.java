package com.whiskey.domain.stock.service;

import com.whiskey.domain.order.Order;
import com.whiskey.domain.stock.Stock;
import com.whiskey.domain.stock.StockReservation;
import com.whiskey.domain.stock.repository.StockRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockReservationService {

    private final StockRepository stockRepository;

    @Retryable(
        value = OptimisticLockException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public void reserveStock(Order order, Stock stock, int quantity, int expireTime) {
        Stock freshStock = stockRepository.findById(stock.getId())
            .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다."));

        StockReservation.create(freshStock, order, quantity, expireTime);
        stockRepository.save(freshStock);
    }
}
