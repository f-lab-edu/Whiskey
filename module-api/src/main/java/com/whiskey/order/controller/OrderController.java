package com.whiskey.order.controller;

import com.whiskey.domain.order.dto.OrderCommand;
import com.whiskey.domain.order.dto.OrderResult;
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
    @Operation(summary = "주문 생성 및 재고 확인", description = "주문을 생성하고 재고를 예약합니다.")
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

        OrderResult result = orderService.createOrder(command);
        CreateOrderResponse response = CreateOrderResponse.from(result);
        return ApiResponse.success("주문 생성에 성공했습니다.", response);
    }

}
