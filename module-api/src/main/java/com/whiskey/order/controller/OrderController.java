package com.whiskey.order.controller;

import com.whiskey.annotation.CurrentMemberId;
import com.whiskey.domain.order.dto.OrderCommand;
import com.whiskey.domain.order.dto.OrderResult;
import com.whiskey.domain.order.service.OrderService;
import com.whiskey.order.dto.CreateOrderRequest;
import com.whiskey.order.dto.CreateOrderResponse;
import com.whiskey.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public ApiResponse<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request, @CurrentMemberId Long memberId) {
        OrderCommand command = new OrderCommand(
            memberId,
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

    @PatchMapping("/order/{orderId}/cancel")
    @Operation(summary = "생성된 주문 취소", description = "생성된 주문을 취소하고 예약된 재고를 반환합니다.")
    public ApiResponse<Void> cancelOrder(@Parameter(description = "주문 ID") @PathVariable("orderId") Long orderId, @CurrentMemberId Long memberId) {
        log.info("memberId={}, orderId={}", memberId, orderId);
        orderService.cancelOrder(orderId, memberId);
        return ApiResponse.success("생성된 주문 취소가 완료되었습니다.");
    }
}
