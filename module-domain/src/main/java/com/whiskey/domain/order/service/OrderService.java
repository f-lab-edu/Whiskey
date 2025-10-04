package com.whiskey.domain.order.service;

import com.whiskey.domain.order.Order;
import com.whiskey.domain.order.dto.OrderCommand;
import com.whiskey.domain.order.dto.OrderResult;
import com.whiskey.domain.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderResult createOrder(OrderCommand orderCommand) {
        return null;
    }
}