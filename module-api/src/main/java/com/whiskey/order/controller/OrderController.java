package com.whiskey.order.controller;

import com.whiskey.domain.order.dto.OrderCommand;
import com.whiskey.domain.order.service.OrderService;
import com.whiskey.order.dto.CreateOrderRequest;
import com.whiskey.order.dto.CreateOrderResponse;
import com.whiskey.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/order")
    @Operation(summary = "주문 전 재고 확인", description = "주문 전 재고를 확인합니다.")
    public ApiResponse<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderCommand command = new OrderCommand(
            request.userId(),
            request.items().stream()
                .map(item -> new OrderCommand.OrderItem(
                    item.stockId(),
                    item.quantity()
                ))
                .toList()
        );

        orderService.createOrder(command);
        return ApiResponse.success("재고 확인에 성공했습니다.");
    }

}
